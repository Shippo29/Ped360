package com.pedidos360.orders.messaging;

import com.pedidos360.orders.client.ProductoClient;
import com.pedidos360.orders.model.EstadoPedido;
import com.pedidos360.orders.model.Pedido;
import com.pedidos360.orders.model.PedidoItem;
import com.pedidos360.orders.repository.PedidoRepository;
import com.pedidos360.orders.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.core.AmqpAdmin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@EnabledIfSystemProperty(named = "pedidos.rabbit.it", matches = "true")
class PedidoServiceRabbitIntegrationTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin rabbitAdmin;

    @MockitoBean
    private ProductoClient productoClient;

    @Value("${pedidos360.rabbit.exchange}")
    private String exchangeName;

    @Value("${pedidos360.rabbit.routing-key}")
    private String routingKey;

    @Value("${pedidos360.rabbit.queues.notificaciones}")
    private String notificacionesQueue;

    @Value("${pedidos360.rabbit.queues.inventario}")
    private String inventarioQueue;

    @Test
    void aceptarPedidoPublicaEventoRealEnRabbitMq() {
        comprobarColaVacia(notificacionesQueue);
        comprobarColaVacia(inventarioQueue);

        String observerQueueName = "ep3.integration." + UUID.randomUUID().toString().replace("-", "");
        Queue observerQueue = new Queue(observerQueueName, false, true, true);
        rabbitAdmin.declareQueue(observerQueue);
        Binding observerBinding = BindingBuilder.bind(observerQueue)
                .to(new TopicExchange(exchangeName))
                .with(routingKey);
        rabbitAdmin.declareBinding(observerBinding);

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

            pedidoService.cambiarEstado(guardado.getId(), EstadoPedido.ACEPTADO);

            Message evento = rabbitTemplate.receive(observerQueueName, 10_000);
            assertNotNull(evento, "El publisher no entregó el evento a RabbitMQ");
            String payload = new String(evento.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(payload.contains("\"pedidoId\":" + guardado.getId()));
            assertTrue(payload.contains("\"estado\":\"ACEPTADO\""));
            confirmarColaDeNegocioSiNoTieneConsumidor(notificacionesQueue, guardado.getId());
            confirmarColaDeNegocioSiNoTieneConsumidor(inventarioQueue, guardado.getId());
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

    private void confirmarColaDeNegocioSiNoTieneConsumidor(String queueName, Long pedidoId) {
        QueueInformation info = rabbitAdmin.getQueueInfo(queueName);
        if (info != null && info.getConsumerCount() == 0) {
            Message evento = rabbitTemplate.receive(queueName, 10_000);
            assertNotNull(evento, "No se encoló el evento en " + queueName);
            String payload = new String(evento.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(payload.contains("\"pedidoId\":" + pedidoId));
        }
    }
}
