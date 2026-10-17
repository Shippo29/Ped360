package com.pedidos360.orders.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
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
        return colaDeNegocio(properties.getQueues().getNotificaciones(), properties);
    }

    @Bean
    Queue cocinaPreparacionQueue(RabbitProperties properties) {
        return colaDeNegocio(properties.getQueues().getCocinaPreparacion(), properties);
    }

    @Bean
    Queue facturacionDocumentosQueue(RabbitProperties properties) {
        return colaDeNegocio(properties.getQueues().getFacturacionDocumentos(), properties);
    }

    @Bean
    Queue deadLetterQueue(RabbitProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getDlq()).build();
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
    FanoutExchange pedidosDeadLetterExchange(RabbitProperties properties) {
        return new FanoutExchange(properties.getDeadLetterExchange());
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
    Binding deadLetterBinding(
            @Qualifier("deadLetterQueue") Queue deadLetterQueue,
            FanoutExchange pedidosDeadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(pedidosDeadLetterExchange);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    private Queue colaDeNegocio(String nombre, RabbitProperties properties) {
        return QueueBuilder.durable(nombre)
                .withArgument("x-dead-letter-exchange", properties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", properties.getDeadLetterRoutingKey())
                .build();
    }
}
