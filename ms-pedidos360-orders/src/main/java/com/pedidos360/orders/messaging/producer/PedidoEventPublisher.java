package com.pedidos360.orders.messaging.producer;

import com.pedidos360.orders.messaging.config.RabbitProperties;
import com.pedidos360.orders.model.Pedido;
import com.pedidos360.orders.model.PedidoItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties properties;

    public void publicarPedidoAceptado(Pedido pedido) {
        try {
            String payload = crearPayload(pedido);
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            Message message = new Message(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8), messageProperties);
            rabbitTemplate.send(properties.getExchange(), properties.getRoutingKey(), message);
        } catch (Exception ex) {
            log.error("No se pudo publicar el evento de aceptación del pedido id={}", pedido.getId(), ex);
        }
    }

    private String crearPayload(Pedido pedido) {
        StringBuilder json = new StringBuilder()
                .append("{\"pedidoId\":").append(pedido.getId())
                .append(",\"clienteId\":\"").append(escaparJson(pedido.getClienteId()))
                .append("\",\"estado\":\"ACEPTADO\",\"items\":[");

        for (int i = 0; i < pedido.getItems().size(); i++) {
            PedidoItem item = pedido.getItems().get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"productoId\":").append(item.getProductoId())
                    .append(",\"nombreProducto\":\"").append(escaparJson(item.getNombreProducto()))
                    .append("\",\"cantidad\":").append(item.getCantidad())
                    .append(",\"precioUnitario\":").append(item.getPrecioUnitario())
                    .append('}');
        }
        return json.append("]}").toString();
    }

    private String escaparJson(String valor) {
        StringBuilder escaped = new StringBuilder();
        for (char caracter : valor.toCharArray()) {
            switch (caracter) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (caracter < 0x20) {
                        String hexadecimal = Integer.toHexString(caracter);
                        escaped.append("\\u").append("0".repeat(4 - hexadecimal.length())).append(hexadecimal);
                    } else {
                        escaped.append(caracter);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
