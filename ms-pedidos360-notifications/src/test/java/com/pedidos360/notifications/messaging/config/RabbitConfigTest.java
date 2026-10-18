package com.pedidos360.notifications.messaging.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RabbitConfigTest {

    @Test
    void asignaUnaDlqYRoutingKeyPorColaOperacional() {
        RabbitProperties properties = new RabbitProperties();
        properties.setDeadLetterExchange("pedidos.dlx");
        properties.getQueues().setNotificaciones("cola.notificaciones");
        properties.getQueues().setCocinaPreparacion("cola.cocina-preparacion");
        properties.getQueues().setFacturacionDocumentos("cola.facturacion-documentos");
        properties.getRoutingKeys().setDlqNotificaciones("pedido.notificacion.dlq");
        properties.getRoutingKeys().setDlqCocinaPreparacion("pedido.cocina.dlq");
        properties.getRoutingKeys().setDlqFacturacionDocumentos("pedido.factura.dlq");
        RabbitConfig config = new RabbitConfig();

        assertDeadLetter(config.notificacionesQueue(properties), "pedido.notificacion.dlq");
        assertDeadLetter(config.cocinaPreparacionQueue(properties), "pedido.cocina.dlq");
        assertDeadLetter(config.facturacionDocumentosQueue(properties), "pedido.factura.dlq");
    }

    private void assertDeadLetter(Queue queue, String routingKey) {
        assertEquals("pedidos.dlx", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(routingKey, queue.getArguments().get("x-dead-letter-routing-key"));
    }
}
