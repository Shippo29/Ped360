package com.pedidos360.orders.messaging.producer;

import com.pedidos360.orders.messaging.config.RabbitProperties;
import com.pedidos360.orders.messaging.dto.PedidoMensaje;
import com.pedidos360.orders.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class PedidoMessageProducerTest {

    @Test
    void publicaUnaVezEnCadaDestinoAlAceptar() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        PedidoMessageProducer producer = new PedidoMessageProducer(rabbitTemplate, properties());

        producer.publicarPedidoAceptado(Pedido.builder().id(25L).clienteId("cliente").build());

        verify(rabbitTemplate).convertAndSend(
                eq("pedidos.direct"), eq("pedido.cocina"), any(PedidoMensaje.class));
        verify(rabbitTemplate).convertAndSend(
                eq("pedidos.direct"), eq("pedido.factura"), any(PedidoMensaje.class));
        verify(rabbitTemplate).convertAndSend(
                eq("pedidos.topic"), eq("pedido.notificacion.aceptado"), any(PedidoMensaje.class));
        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void unFalloDeRabbitNoSePropagaAlServicio() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        doThrow(new AmqpConnectException(new IllegalStateException("broker no disponible")))
                .when(rabbitTemplate).convertAndSend(
                        eq("pedidos.topic"),
                        eq("pedido.notificacion.creado"),
                        any(PedidoMensaje.class));
        PedidoMessageProducer producer = new PedidoMessageProducer(rabbitTemplate, properties());

        assertDoesNotThrow(() ->
                producer.publicarPedidoCreado(Pedido.builder().id(26L).clienteId("cliente").build()));
    }

    private RabbitProperties properties() {
        RabbitProperties properties = new RabbitProperties();
        properties.setDirectExchange("pedidos.direct");
        properties.setTopicExchange("pedidos.topic");
        RabbitProperties.RoutingKeys keys = new RabbitProperties.RoutingKeys();
        keys.setCocina("pedido.cocina");
        keys.setFactura("pedido.factura");
        keys.setNotificacionCreado("pedido.notificacion.creado");
        keys.setNotificacionAceptado("pedido.notificacion.aceptado");
        properties.setRoutingKeys(keys);
        return properties;
    }
}
