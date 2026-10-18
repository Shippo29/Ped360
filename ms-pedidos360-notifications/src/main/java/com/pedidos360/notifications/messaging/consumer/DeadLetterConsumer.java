package com.pedidos360.notifications.messaging.consumer;

import com.pedidos360.notifications.messaging.dto.PedidoMensaje;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Map;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "pedidos360.rabbit", name = "dlq-consumer-enabled",
        havingValue = "true", matchIfMissing = true)
public class DeadLetterConsumer extends AckingPedidoConsumer {

    @RabbitListener(queues = {
            "${pedidos360.rabbit.queues.dlq-notificaciones}",
            "${pedidos360.rabbit.queues.dlq-cocina-preparacion}",
            "${pedidos360.rabbit.queues.dlq-facturacion-documentos}"
    })
    public void recibir(PedidoMensaje mensaje, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                        @Headers Map<String, Object> headers) {
        procesarConAck("DLQ", mensaje, channel, tag, () ->
                log.error("Mensaje recibido en DLQ; headers={}, payload={}", headers, mensaje));
    }
}
