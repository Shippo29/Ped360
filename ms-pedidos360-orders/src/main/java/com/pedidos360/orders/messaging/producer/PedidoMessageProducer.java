package com.pedidos360.orders.messaging.producer;

import com.pedidos360.orders.messaging.config.RabbitProperties;
import com.pedidos360.orders.messaging.dto.PedidoMensaje;
import com.pedidos360.orders.model.Pedido;
import com.pedidos360.orders.model.PedidoItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties properties;

    public void publicarPedidoCreado(Pedido pedido) {
        publicar(properties.getTopicExchange(),
                properties.getRoutingKeys().getNotificacionCreado(),
                mensaje(pedido, "Pedido creado", false));
    }

    public void publicarPedidoAceptado(Pedido pedido) {
        PedidoMensaje comando = mensaje(pedido, "Pedido aceptado", false);
        publicar(properties.getDirectExchange(), properties.getRoutingKeys().getCocina(), comando);
        publicar(properties.getDirectExchange(), properties.getRoutingKeys().getFactura(), comando);
        publicar(properties.getTopicExchange(),
                properties.getRoutingKeys().getNotificacionAceptado(), comando);
    }

    public void publicarPedidoDespachado(Pedido pedido) {
        publicar(properties.getTopicExchange(),
                properties.getRoutingKeys().getNotificacionDespachado(),
                mensaje(pedido, "Pedido despachado", false));
    }

    public void publicarMensajePrueba(Pedido pedido) {
        publicar(properties.getTopicExchange(),
                properties.getRoutingKeys().getNotificacionCreado(),
                mensaje(pedido, "Prueba de envío a DLQ", true));
    }

    private PedidoMensaje mensaje(Pedido pedido, String detalle, boolean forzarFallo) {
        return PedidoMensaje.builder()
                .pedidoId(pedido.getId())
                .clienteId(pedido.getClienteId())
                .estado(pedido.getEstado().name())
                .total(pedido.calcularTotal())
                .fecha(Instant.now())
                .mensaje(detalle)
                .forzarFalloParaDlq(forzarFallo)
                .items(pedido.getItems().stream().map(this::itemMensaje).toList())
                .build();
    }

    private PedidoMensaje.ItemMensaje itemMensaje(PedidoItem item) {
        return PedidoMensaje.ItemMensaje.builder()
                .productoId(item.getProductoId())
                .nombreProducto(item.getNombreProducto())
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .build();
    }

    private void publicar(String exchange, String routingKey, PedidoMensaje mensaje) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, mensaje);
        } catch (Exception ex) {
            log.error("No se pudo publicar el evento del pedido id={} en {} con routing key {}",
                    mensaje.getPedidoId(), exchange, routingKey, ex);
        }
    }
}
