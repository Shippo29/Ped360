package com.pedidos360.orders.messaging.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class NotificacionConsumer {

    @RabbitListener(queues = "${pedidos360.rabbit.queues.notificaciones}")
    public void recibir(
            String payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            if (payload.contains("FALLAR")) {
                throw new IllegalStateException("Fallo controlado para demostrar el envío a la DLQ");
            }
            log.info("Evento de pedido recibido para notificaciones: {}", payload);
            // ACK confirma que la notificación se procesó correctamente.
            channel.basicAck(tag, false);
            log.info("ACK de notificación completado. deliveryTag={}", tag);
        } catch (Exception ex) {
            log.error("Falló el procesamiento de notificaciones. payload={}", payload, ex);
            rechazar(channel, tag, payload);
        }
    }

    private void rechazar(Channel channel, long tag, String payload) {
        try {
            // NACK sin requeue envía el mensaje a la DLQ configurada en la cola.
            channel.basicNack(tag, false, false);
            log.warn("NACK de notificación completado sin requeue. deliveryTag={}", tag);
        } catch (IOException ex) {
            log.error("No se pudo rechazar el mensaje de notificación. payload={}", payload, ex);
        }
    }
}
