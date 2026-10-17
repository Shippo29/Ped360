package com.pedidos360.notifications.messaging.consumer;

import com.pedidos360.notifications.messaging.dto.PedidoMensaje;
import com.pedidos360.notifications.service.NotificacionTrackerService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificacionesConsumer extends AckingPedidoConsumer {

    private final NotificacionTrackerService trackerService;

    @RabbitListener(queues = "${pedidos360.rabbit.queues.notificaciones}")
    public void recibir(PedidoMensaje mensaje, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        procesarConAck("notificaciones", mensaje, channel, tag, () -> {
            if (mensaje.isForzarFalloParaDlq()) {
                throw new IllegalStateException("Fallo forzado para probar el envío a la DLQ");
            }
            trackerService.registrar(mensaje.getEstado(), mensaje.getMensaje(), mensaje);
        });
    }
}
