package com.pedidos360.notifications.messaging.consumer;

import com.pedidos360.notifications.messaging.dto.PedidoMensaje;
import com.pedidos360.notifications.service.NotificacionTrackerService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificacionesConsumerTest {

    @Test
    void ackAlRegistrarUnaNotificacion() throws IOException {
        Channel channel = mock(Channel.class);
        NotificacionTrackerService tracker = new NotificacionTrackerService();
        NotificacionesConsumer consumer = new NotificacionesConsumer(tracker);
        PedidoMensaje mensaje = PedidoMensaje.builder()
                .pedidoId(17L)
                .estado("ACEPTADO")
                .mensaje("Pedido aceptado")
                .build();

        consumer.recibir(mensaje, channel, 4L);

        verify(channel).basicAck(4L, false);
        assertEquals(1, tracker.listar().size());
        assertEquals(17L, tracker.listar().getFirst().pedidoId());
    }

    @Test
    void nackSinRequeueCuandoSeFuerzaElFallo() throws IOException {
        Channel channel = mock(Channel.class);
        NotificacionesConsumer consumer = new NotificacionesConsumer(new NotificacionTrackerService());
        PedidoMensaje mensaje = PedidoMensaje.builder()
                .pedidoId(18L)
                .forzarFalloParaDlq(true)
                .build();

        consumer.recibir(mensaje, channel, 5L);

        verify(channel).basicNack(5L, false, false);
    }
}
