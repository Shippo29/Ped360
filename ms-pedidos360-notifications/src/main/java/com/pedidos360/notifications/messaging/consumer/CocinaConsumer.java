package com.pedidos360.notifications.messaging.consumer;

import com.pedidos360.notifications.messaging.dto.PedidoMensaje;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CocinaConsumer extends AckingPedidoConsumer {

    @RabbitListener(queues = "${pedidos360.rabbit.queues.cocina-preparacion}")
    public void recibir(PedidoMensaje mensaje, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        procesarConAck("cocina", mensaje, channel, tag,
                () -> log.info("Comando de cocina recibido para el pedido {}", mensaje.getPedidoId()));
    }
}
