package com.pedidos360.notifications.messaging.consumer;

import com.pedidos360.notifications.messaging.dto.PedidoMensaje;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class AckingPedidoConsumer {

    protected void procesarConAck(
            String dominio,
            PedidoMensaje mensaje,
            Channel channel,
            long tag,
            Runnable procesamiento) {
        try {
            procesamiento.run();
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.error("Falló el procesamiento de {} para pedido {}", dominio, mensaje.getPedidoId(), ex);
            try {
                channel.basicNack(tag, false, false);
            } catch (IOException nackEx) {
                log.error("No se pudo enviar NACK de {} para pedido {}", dominio, mensaje.getPedidoId(), nackEx);
            }
        }
    }

}
