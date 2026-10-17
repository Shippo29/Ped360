package com.pedidos360.notifications.messaging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pedidos360.rabbit")
public class RabbitProperties {
    private String directExchange;
    private String topicExchange;
    private String deadLetterExchange;
    private String deadLetterRoutingKey;
    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {
        private String notificaciones;
        private String cocinaPreparacion;
        private String facturacionDocumentos;
        private String dlq;
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        private String cocina;
        private String factura;
        private String notificacionCreado;
        private String notificacionAceptado;
        private String notificacionDespachado;
        private String patronNotificaciones;
    }
}
