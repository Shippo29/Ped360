package com.pedidos360.orders.service;

import com.pedidos360.orders.client.ProductoClient;
import com.pedidos360.orders.client.dto.DescontarStockBatchRequest;
import com.pedidos360.orders.client.dto.ProductoDTO;
import com.pedidos360.orders.dto.CrearPedidoRequest;
import com.pedidos360.orders.dto.PedidoResponse;
import com.pedidos360.orders.exception.RecursoNoEncontradoException;
import com.pedidos360.orders.exception.TransicionEstadoInvalidaException;
import com.pedidos360.orders.messaging.producer.PedidoMessageProducer;
import com.pedidos360.orders.model.EstadoPedido;
import com.pedidos360.orders.model.Pedido;
import com.pedidos360.orders.model.PedidoItem;
import com.pedidos360.orders.repository.PedidoRepository;
import com.pedidos360.orders.security.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoClient productoClient;
    private final ActorContext actorContext;
    private final PedidoMessageProducer pedidoMessageProducer;

    @Transactional
    public PedidoResponse crear(CrearPedidoRequest request) {
        Pedido pedido = Pedido.builder()
                .clienteId(resolverClienteId(request))
                .estado(EstadoPedido.CREADO)
                .fechaCreacion(Instant.now())
                .build();

        for (CrearPedidoRequest.ItemPedidoRequest itemReq : request.getItems()) {
            ProductoDTO producto = productoClient.obtenerProducto(itemReq.getProductoId());

            PedidoItem item = PedidoItem.builder()
                    .productoId(producto.getId())
                    .nombreProducto(producto.getNombre())
                    .cantidad(itemReq.getCantidad())
                    .precioUnitario(producto.getPrecio())
                    .build();
            pedido.agregarItem(item);
        }

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        pedidoMessageProducer.publicarPedidoCreado(pedidoGuardado);
        return PedidoResponse.desde(pedidoGuardado);
    }


    private String resolverClienteId(CrearPedidoRequest request) {
        String idActor = actorContext.obtenerIdActor();
        boolean puedeAsignarAOtroCliente = actorContext.esOperador() || actorContext.esAdmin();

        if (puedeAsignarAOtroCliente
                && request.getClienteId() != null
                && !request.getClienteId().isBlank()) {
            return request.getClienteId();
        }
        return idActor;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarSegunActor() {
        List<Pedido> pedidos = actorContext.esCliente()
                ? pedidoRepository.findByClienteId(actorContext.obtenerIdActor())
                : pedidoRepository.findAll();

        return pedidos.stream().map(PedidoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtenerDetalle(Long id) {
        Pedido pedido = buscarPorId(id);

        if (actorContext.esCliente() && !pedido.getClienteId().equals(actorContext.obtenerIdActor())) {
            throw new AccessDeniedException("No puedes consultar pedidos de otro cliente");
        }

        return PedidoResponse.desde(pedido);
    }

    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = buscarPorId(id);
        EstadoPedido estadoActual = pedido.getEstado();

        if (!estadoActual.puedeTransicionarA(nuevoEstado)) {
            throw new TransicionEstadoInvalidaException(
                    "No se puede pasar de " + estadoActual + " a " + nuevoEstado
                            + " (recuerda: un pedido no puede DESPACHARSE si no fue ACEPTADO antes)");
        }

        if (nuevoEstado == EstadoPedido.ACEPTADO) {
            List<DescontarStockBatchRequest.ItemDescontar> items = pedido.getItems().stream()
                    .map(item -> new DescontarStockBatchRequest.ItemDescontar(
                            item.getProductoId(), item.getCantidad()))
                    .toList();
            productoClient.descontarStockBatch(items);
        }

        pedido.setEstado(nuevoEstado);
        pedido.setFechaActualizacion(Instant.now());
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        if (nuevoEstado == EstadoPedido.ACEPTADO) {
            pedidoMessageProducer.publicarPedidoAceptado(pedidoGuardado);
        } else if (nuevoEstado == EstadoPedido.DESPACHADO) {
            pedidoMessageProducer.publicarPedidoDespachado(pedidoGuardado);
        }

        return PedidoResponse.desde(pedidoGuardado);
    }

    @Transactional(readOnly = true)
    public void simularErrorDlq(Long id) {
        pedidoMessageProducer.publicarMensajePrueba(buscarPorId(id));
    }

    private Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: id=" + id));
    }
}
