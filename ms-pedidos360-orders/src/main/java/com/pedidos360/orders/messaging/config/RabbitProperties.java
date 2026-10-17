package com.pedidos360.orders.messaging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pedidos360.rabbit")
public class RabbitProperties {

    private String directExchange = "pedidos.direct";
    private String topicExchange = "pedidos.topic";
    private String deadLetterExchange = "pedidos.dlx";
    private String deadLetterRoutingKey = "pedido.dlq";
    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {
        private String notificaciones = "cola.notificaciones";
        private String cocinaPreparacion = "cola.cocina-preparacion";
        private String facturacionDocumentos = "cola.facturacion-documentos";
        private String dlq = "cola.pedidos.dlq";
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        private String cocina = "pedido.cocina";
        private String factura = "pedido.factura";
        private String notificacionCreado = "pedido.notificacion.creado";
        private String notificacionAceptado = "pedido.notificacion.aceptado";
        private String notificacionDespachado = "pedido.notificacion.despachado";
        private String patronNotificaciones = "pedido.notificacion.*";
        private String deadLetter = "pedido.dlq";
    }
}
