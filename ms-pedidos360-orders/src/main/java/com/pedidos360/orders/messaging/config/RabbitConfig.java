package com.pedidos360.orders.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitConfig {

    @Bean
    Queue notificacionesQueue(RabbitProperties properties) {
        return colaDeNegocio(properties.getQueues().getNotificaciones(),
                properties.getRoutingKeys().getDlqNotificaciones(), properties);
    }

    @Bean
    Queue cocinaPreparacionQueue(RabbitProperties properties) {
        return colaDeNegocio(properties.getQueues().getCocinaPreparacion(),
                properties.getRoutingKeys().getDlqCocinaPreparacion(), properties);
    }

    @Bean
    Queue facturacionDocumentosQueue(RabbitProperties properties) {
        return colaDeNegocio(properties.getQueues().getFacturacionDocumentos(),
                properties.getRoutingKeys().getDlqFacturacionDocumentos(), properties);
    }

    @Bean
    Queue notificacionesDeadLetterQueue(RabbitProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getDlqNotificaciones()).build();
    }

    @Bean
    Queue cocinaDeadLetterQueue(RabbitProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getDlqCocinaPreparacion()).build();
    }

    @Bean
    Queue facturacionDeadLetterQueue(RabbitProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getDlqFacturacionDocumentos()).build();
    }

    @Bean
    DirectExchange pedidosDirectExchange(RabbitProperties properties) {
        return new DirectExchange(properties.getDirectExchange());
    }

    @Bean
    TopicExchange pedidosTopicExchange(RabbitProperties properties) {
        return new TopicExchange(properties.getTopicExchange());
    }

    @Bean
    DirectExchange pedidosDeadLetterExchange(RabbitProperties properties) {
        return new DirectExchange(properties.getDeadLetterExchange());
    }

    @Bean
    Binding cocinaPreparacionBinding(
            @Qualifier("cocinaPreparacionQueue") Queue cocinaPreparacionQueue,
            DirectExchange pedidosDirectExchange,
            RabbitProperties properties) {
        return BindingBuilder.bind(cocinaPreparacionQueue)
                .to(pedidosDirectExchange)
                .with(properties.getRoutingKeys().getCocina());
    }

    @Bean
    Binding facturacionDocumentosBinding(
            @Qualifier("facturacionDocumentosQueue") Queue facturacionDocumentosQueue,
            DirectExchange pedidosDirectExchange,
            RabbitProperties properties) {
        return BindingBuilder.bind(facturacionDocumentosQueue)
                .to(pedidosDirectExchange)
                .with(properties.getRoutingKeys().getFactura());
    }

    @Bean
    Binding notificacionesBinding(
            @Qualifier("notificacionesQueue") Queue notificacionesQueue,
            TopicExchange pedidosTopicExchange,
            RabbitProperties properties) {
        return BindingBuilder.bind(notificacionesQueue)
                .to(pedidosTopicExchange)
                .with(properties.getRoutingKeys().getPatronNotificaciones());
    }

    @Bean
    Binding notificacionesDeadLetterBinding(
            @Qualifier("notificacionesDeadLetterQueue") Queue deadLetterQueue,
            DirectExchange pedidosDeadLetterExchange,
            RabbitProperties properties) {
        return BindingBuilder.bind(deadLetterQueue).to(pedidosDeadLetterExchange)
                .with(properties.getRoutingKeys().getDlqNotificaciones());
    }

    @Bean
    Binding cocinaDeadLetterBinding(
            @Qualifier("cocinaDeadLetterQueue") Queue deadLetterQueue,
            DirectExchange pedidosDeadLetterExchange,
            RabbitProperties properties) {
        return BindingBuilder.bind(deadLetterQueue).to(pedidosDeadLetterExchange)
                .with(properties.getRoutingKeys().getDlqCocinaPreparacion());
    }

    @Bean
    Binding facturacionDeadLetterBinding(
            @Qualifier("facturacionDeadLetterQueue") Queue deadLetterQueue,
            DirectExchange pedidosDeadLetterExchange,
            RabbitProperties properties) {
        return BindingBuilder.bind(deadLetterQueue).to(pedidosDeadLetterExchange)
                .with(properties.getRoutingKeys().getDlqFacturacionDocumentos());
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    private Queue colaDeNegocio(String nombre, String deadLetterRoutingKey, RabbitProperties properties) {
        return QueueBuilder.durable(nombre)
                .withArgument("x-dead-letter-exchange", properties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
                .build();
    }
}
