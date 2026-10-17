package com.pedidos360.orders.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitConfig {

    // Las colas de negocio envían los mensajes rechazados a la DLX compartida.
    @Bean
    Queue notificacionesQueue(RabbitProperties properties) {
        return colaDeNegocio(properties.getQueues().getNotificaciones(), properties);
    }

    @Bean
    Queue inventarioQueue(RabbitProperties properties) {
        return colaDeNegocio(properties.getQueues().getInventario(), properties);
    }

    @Bean
    Queue deadLetterQueue(RabbitProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getDlq()).build();
    }

    @Bean
    TopicExchange pedidosExchange(RabbitProperties properties) {
        return new TopicExchange(properties.getExchange());
    }

    @Bean
    DirectExchange pedidosDeadLetterExchange(RabbitProperties properties) {
        return new DirectExchange(properties.getDlx());
    }

    @Bean
    Binding notificacionesBinding(
            Queue notificacionesQueue,
            TopicExchange pedidosExchange,
            RabbitProperties properties) {
        return BindingBuilder.bind(notificacionesQueue)
                .to(pedidosExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    Binding inventarioBinding(
            Queue inventarioQueue,
            TopicExchange pedidosExchange,
            RabbitProperties properties) {
        return BindingBuilder.bind(inventarioQueue)
                .to(pedidosExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    Binding deadLetterBinding(
            Queue deadLetterQueue,
            DirectExchange pedidosDeadLetterExchange,
            RabbitProperties properties) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(pedidosDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    private Queue colaDeNegocio(String nombre, RabbitProperties properties) {
        return QueueBuilder.durable(nombre)
                .withArgument("x-dead-letter-exchange", properties.getDlx())
                .withArgument("x-dead-letter-routing-key", properties.getDeadLetterRoutingKey())
                .build();
    }
}
