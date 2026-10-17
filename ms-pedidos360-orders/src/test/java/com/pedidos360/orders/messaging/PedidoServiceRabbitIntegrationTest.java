package com.pedidos360.orders.messaging;

import com.pedidos360.orders.client.ProductoClient;
import com.pedidos360.orders.model.EstadoPedido;
import com.pedidos360.orders.model.Pedido;
import com.pedidos360.orders.model.PedidoItem;
import com.pedidos360.orders.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "pedidos.rabbit.it", matches = "true")
class PedidoServiceRabbitIntegrationTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin rabbitAdmin;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoClient productoClient;

    @Value("${pedidos360.rabbit.topic-exchange}")
    private String exchangeName;

    @Value("${pedidos360.rabbit.direct-exchange}")
    private String directExchangeName;

    @Value("${pedidos360.rabbit.routing-keys.notificacion-aceptado}")
    private String routingKey;

    @Value("${pedidos360.rabbit.routing-keys.cocina}")
    private String cocinaRoutingKey;

    @Value("${pedidos360.rabbit.routing-keys.factura}")
    private String facturaRoutingKey;

    @Value("${pedidos360.rabbit.queues.notificaciones}")
    private String notificacionesQueue;

    @Value("${pedidos360.rabbit.queues.cocina-preparacion}")
    private String cocinaQueue;

    @Value("${pedidos360.rabbit.queues.facturacion-documentos}")
    private String facturacionQueue;

    @Value("${pedidos360.rabbit.queues.dlq}")
    private String deadLetterQueue;

    @Value("${pedidos360.rabbit.dead-letter-exchange}")
    private String deadLetterExchange;

    @Test
    void aceptarPedidoPublicaEventoRealEnRabbitMq() throws Exception {
        comprobarColaVacia(notificacionesQueue);
        comprobarColaVacia(cocinaQueue);
        comprobarColaVacia(facturacionQueue);

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String notificationsObserverName = "ep3.integration.notifications." + suffix;
        String cocinaObserverName = "ep3.integration.cocina." + suffix;
        String facturacionObserverName = "ep3.integration.facturacion." + suffix;
        Queue notificationsObserver = new Queue(notificationsObserverName, false, false, false);
        Queue cocinaObserver = new Queue(cocinaObserverName, false, false, false);
        Queue facturacionObserver = new Queue(facturacionObserverName, false, false, false);
        rabbitAdmin.declareQueue(notificationsObserver);
        rabbitAdmin.declareQueue(cocinaObserver);
        rabbitAdmin.declareQueue(facturacionObserver);
        Binding notificationsObserverBinding = BindingBuilder.bind(notificationsObserver)
                .to(new TopicExchange(exchangeName)).with(routingKey);
        Binding cocinaObserverBinding = BindingBuilder.bind(cocinaObserver)
                .to(new DirectExchange(directExchangeName)).with(cocinaRoutingKey);
        Binding facturacionObserverBinding = BindingBuilder.bind(facturacionObserver)
                .to(new DirectExchange(directExchangeName)).with(facturaRoutingKey);
        rabbitAdmin.declareBinding(notificationsObserverBinding);
        rabbitAdmin.declareBinding(cocinaObserverBinding);
        rabbitAdmin.declareBinding(facturacionObserverBinding);

        try {
            Pedido pedido = Pedido.builder()
                    .clienteId("ep3-integration")
                    .estado(EstadoPedido.CREADO)
                    .fechaCreacion(Instant.now())
                    .build();
            pedido.agregarItem(PedidoItem.builder()
                    .productoId(360L)
                    .nombreProducto("Producto de integración")
                    .cantidad(1)
                    .precioUnitario(new BigDecimal("12.50"))
                    .build());
            Pedido guardado = pedidoRepository.save(pedido);
            doNothing().when(productoClient).descontarStockBatch(anyList());

            mockMvc.perform(put("/api/orders/{id}/status", guardado.getId())
                            .with(user("operador").roles("Operador"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nuevoEstado\":\"ACEPTADO\"}"))
                    .andExpect(status().isOk());

            comprobarMensajeUnico(notificationsObserverName, guardado.getId());
            comprobarMensajeUnico(cocinaObserverName, guardado.getId());
            comprobarMensajeUnico(facturacionObserverName, guardado.getId());
            confirmarColaDeNegocioSiNoTieneConsumidor(notificacionesQueue, guardado.getId());
            confirmarColaDeNegocioSiNoTieneConsumidor(cocinaQueue, guardado.getId());
            confirmarColaDeNegocioSiNoTieneConsumidor(facturacionQueue, guardado.getId());
        } finally {
            rabbitAdmin.removeBinding(notificationsObserverBinding);
            rabbitAdmin.removeBinding(cocinaObserverBinding);
            rabbitAdmin.removeBinding(facturacionObserverBinding);
            rabbitAdmin.deleteQueue(notificationsObserverName);
            rabbitAdmin.deleteQueue(cocinaObserverName);
            rabbitAdmin.deleteQueue(facturacionObserverName);
        }
    }

    @Test
    void endpointDePruebaProvocaNackYEnrutaUnaCopiaALaDlq() throws Exception {
        comprobarColaVacia(deadLetterQueue);
        String observerQueueName = "ep3.dlq.integration." + UUID.randomUUID().toString().replace("-", "");
        Queue observerQueue = new Queue(observerQueueName, false, false, false);
        rabbitAdmin.declareQueue(observerQueue);
        Binding observerBinding = BindingBuilder.bind(observerQueue)
                .to(new FanoutExchange(deadLetterExchange));
        rabbitAdmin.declareBinding(observerBinding);

        try {
            Pedido pedido = Pedido.builder()
                    .clienteId("ep3-dlq-integration")
                    .estado(EstadoPedido.CREADO)
                    .fechaCreacion(Instant.now())
                    .build();
            Pedido guardado = pedidoRepository.save(pedido);

            mockMvc.perform(post("/api/orders/{id}/simular-error-dlq", guardado.getId())
                            .with(user("operador").roles("Operador")))
                    .andExpect(status().isAccepted());

            Message mensajeFallido = rabbitTemplate.receive(observerQueueName, 10_000);
            assertNotNull(mensajeFallido, "El NACK no publicó en el exchange fanout de DLQ");
            String payload = new String(mensajeFallido.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(payload.contains("\"forzarFalloParaDlq\":true"));
            Object xDeathHeader = mensajeFallido.getMessageProperties().getHeaders().get("x-death");
            assertTrue(xDeathHeader instanceof List<?>, "El mensaje debe incluir el header x-death");
            List<?> xDeath = (List<?>) xDeathHeader;
            assertFalse(xDeath.isEmpty(), "El header x-death debe incluir el rechazo");
            assertTrue(xDeath.get(0) instanceof Map<?, ?>, "La entrada x-death debe ser un mapa");
            Map<?, ?> death = (Map<?, ?>) xDeath.get(0);
            assertTrue(death.get("count") instanceof Number, "x-death debe incluir un count numérico");
            assertEquals(1L, ((Number) death.get("count")).longValue(),
                    "El NACK sin requeue debe enviar el mensaje a la DLQ una sola vez");
            assertNull(rabbitTemplate.receive(observerQueueName, 100),
                    "El mensaje forzado debe llegar una sola vez a la DLQ");
            esperarColaVacia(deadLetterQueue);
        } finally {
            rabbitAdmin.removeBinding(observerBinding);
            rabbitAdmin.deleteQueue(observerQueueName);
        }
    }

    private void comprobarColaVacia(String queueName) {
        QueueInformation info = rabbitAdmin.getQueueInfo(queueName);
        assertNotNull(info, "La cola de negocio no existe: " + queueName);
        assertEquals(0, info.getMessageCount(),
                "La cola debe estar vacía antes de ejecutar la prueba: " + queueName);
    }

    private void comprobarMensajeUnico(String queueName, Long pedidoId) {
        QueueInformation info = rabbitAdmin.getQueueInfo(queueName);
        assertNotNull(info, "La cola observadora no existe: " + queueName);
        Message evento = rabbitTemplate.receive(queueName, 10_000);
        assertNotNull(evento, "Debe enrutarse exactamente un mensaje a " + queueName);
        String payload = new String(evento.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"pedidoId\":" + pedidoId));
        assertNull(rabbitTemplate.receive(queueName, 100),
                "No debe haber mensajes duplicados en " + queueName);
    }

    private void confirmarColaDeNegocioSiNoTieneConsumidor(String queueName, Long pedidoId) {
        QueueInformation info = rabbitAdmin.getQueueInfo(queueName);
        assertNotNull(info, "La cola no existe: " + queueName);
        if (info.getConsumerCount() == 0) {
            assertEquals(1, info.getMessageCount(),
                    "Debe haber exactamente un mensaje en " + queueName);
            Message evento = rabbitTemplate.receive(queueName, 10_000);
            assertNotNull(evento, "No se encoló el evento en " + queueName);
            String payload = new String(evento.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(payload.contains("\"pedidoId\":" + pedidoId));
            assertNull(rabbitTemplate.receive(queueName, 100),
                    "No debe haber un mensaje duplicado en " + queueName);
        } else {
            esperarColaVacia(queueName);
        }
    }

    private void esperarColaVacia(String queueName) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        QueueInformation info;
        do {
            info = rabbitAdmin.getQueueInfo(queueName);
            if (info != null && info.getConsumerCount() > 0 && info.getMessageCount() == 0) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrumpida la espera de ACK de " + queueName, ex);
            }
        } while (System.nanoTime() < deadline);
        assertNotNull(info, "La cola no existe: " + queueName);
        assertEquals(0, info.getMessageCount(), "La cola debe quedar vacía tras ACK: " + queueName);
    }
}
