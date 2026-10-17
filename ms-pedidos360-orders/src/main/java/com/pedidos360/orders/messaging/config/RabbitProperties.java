package com.pedidos360.orders.messaging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pedidos360.rabbit")
public class RabbitProperties {

    private String exchange;
    private String dlx;
    private String routingKey;
    private String deadLetterRoutingKey;
    private Queues queues = new Queues();

    @Getter
    @Setter
    public static class Queues {
        private String notificaciones;
        private String inventario;
        private String dlq;
    }
}
