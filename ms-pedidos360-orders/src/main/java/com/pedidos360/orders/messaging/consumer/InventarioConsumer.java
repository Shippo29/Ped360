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
public class InventarioConsumer {

    @RabbitListener(queues = "${pedidos360.rabbit.queues.inventario}")
    public void recibir(
            String payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Evento de pedido recibido para inventario: {}", payload);
            // ACK confirma que el evento de inventario se procesó correctamente.
            channel.basicAck(tag, false);
            log.info("ACK de inventario completado. deliveryTag={}", tag);
        } catch (Exception ex) {
            log.error("Falló el procesamiento de inventario. payload={}", payload, ex);
            rechazar(channel, tag, payload);
        }
    }

    private void rechazar(Channel channel, long tag, String payload) {
        try {
            // NACK sin requeue envía el mensaje fallido a la DLQ.
            channel.basicNack(tag, false, false);
            log.warn("NACK de inventario completado sin requeue. deliveryTag={}", tag);
        } catch (IOException ex) {
            log.error("No se pudo rechazar el mensaje de inventario. payload={}", payload, ex);
        }
    }
}
