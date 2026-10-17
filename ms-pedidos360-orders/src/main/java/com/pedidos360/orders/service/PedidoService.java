package com.pedidos360.orders.service;

import com.pedidos360.orders.client.ProductoClient;
import com.pedidos360.orders.client.dto.DescontarStockBatchRequest;
import com.pedidos360.orders.client.dto.ProductoDTO;
import com.pedidos360.orders.dto.CrearPedidoRequest;
import com.pedidos360.orders.dto.PedidoResponse;
import com.pedidos360.orders.exception.RecursoNoEncontradoException;
import com.pedidos360.orders.exception.TransicionEstadoInvalidaException;
import com.pedidos360.orders.messaging.producer.PedidoEventPublisher;
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
    private final PedidoEventPublisher pedidoEventPublisher;

    @Transactional
    public PedidoResponse crear(CrearPedidoRequest request) {
        Pedido pedido = Pedido.builder()
                .clienteId(resolverClienteId(request))
                .estado(EstadoPedido.CREADO)
                .fechaCreacion(Instant.now())
                .build();

        for (CrearPedidoRequest.ItemPedidoRequest itemReq : request.getItems()) {
            // Se consulta Catálogo (otro servicio) para obtener nombre y precio
            // vigentes; se "congelan" en el pedido al momento de crearlo.
            ProductoDTO producto = productoClient.obtenerProducto(itemReq.getProductoId());

            PedidoItem item = PedidoItem.builder()
                    .productoId(producto.getId())
                    .nombreProducto(producto.getNombre())
                    .cantidad(itemReq.getCantidad())
                    .precioUnitario(producto.getPrecio())
                    .build();
            pedido.agregarItem(item);
        }

        return PedidoResponse.desde(pedidoRepository.save(pedido));
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

    /**
     * Lista pedidos según el actor autenticado: un CLIENTE solo ve los suyos;
     * ADMIN/OPERADOR ven todos (para supervisión y gestión operativa).
     */
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

        // Regla mínima: al aceptar el pedido, se descuenta el stock en Catálogo.
        // Se envían TODOS los ítems en una sola llamada (ver
        // ProductoClient.descontarStockBatch / ProductoService.descontarStockBatch
        // en Catálogo), que los aplica como una única transacción atómica allá:
        // o se descuentan todos los ítems, o no se descuenta ninguno. Esto evita
        // el problema que había antes al llamar item por item (una llamada HTTP
        // por ítem): si el ítem 2 fallaba por falta de stock, el descuento del
        // ítem 1 quedaba aplicado en la base de datos de Catálogo aunque esta
        // transacción local de Orders revirtiera el cambio de estado del pedido
        // (dos servicios, dos bases de datos distintas, sin transacción
        // distribuida entre ambas).
        //
        // Nota: esta llamada sigue sin ser parte de la misma transacción de BD
        // que el cambio de estado de este servicio (son dos servicios, dos
        // bases de datos). Un fallo de red *después* de que Catálogo confirme
        // el descuento (pero antes de que la respuesta llegue aquí) podría
        // dejar el pedido sin actualizar su estado aunque el stock ya se haya
        // descontado: ese caso residual es el tipo de escenario que en EP2 se
        // resuelve mejor con un evento en RabbitMQ (con reintentos/idempotencia)
        // que con esta llamada síncrona.
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
            pedidoEventPublisher.publicarPedidoAceptado(pedidoGuardado);
        }

        return PedidoResponse.desde(pedidoGuardado);
    }

    private Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: id=" + id));
    }
}
