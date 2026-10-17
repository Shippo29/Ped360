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
public class DeadLetterConsumer {

    @RabbitListener(queues = "${pedidos360.rabbit.queues.dlq}")
    public void recibir(
            String payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            // La DLQ registra el payload fallido y confirma que quedó atendido.
            log.error("Mensaje recibido en la DLQ: {}", payload);
            channel.basicAck(tag, false);
            log.info("ACK de DLQ completado. deliveryTag={}", tag);
        } catch (Exception ex) {
            log.error("No se pudo registrar el mensaje de la DLQ. payload={}", payload, ex);
            try {
                channel.basicNack(tag, false, false);
                log.warn("NACK de DLQ completado sin requeue. deliveryTag={}", tag);
            } catch (IOException nackEx) {
                log.error("No se pudo rechazar el mensaje de la DLQ. payload={}", payload, nackEx);
            }
        }
    }
}
