package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.dto.BindingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private AmqpAdmin rabbitAdmin;

    @Mock
    private RabbitManagementClient managementClient;

    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        service = new RabbitAdminService(rabbitAdmin, managementClient);
    }

    @Test
    void declaraColaDurable() {
        service.crearCola("orders.queue");

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(rabbitAdmin).declareQueue(captor.capture());
        assertEquals("orders.queue", captor.getValue().getName());
        assertTrue(captor.getValue().isDurable());
    }

    @Test
    void eliminaColaYExchange() {
        service.eliminarCola("orders.queue");
        service.eliminarExchange("orders.exchange");

        verify(rabbitAdmin).deleteQueue("orders.queue");
        verify(rabbitAdmin).deleteExchange("orders.exchange");
    }

    @Test
    void creaExchangeDelTipoSolicitado() {
        service.crearExchange("orders.exchange", "direct");

        ArgumentCaptor<org.springframework.amqp.core.Exchange> captor =
                ArgumentCaptor.forClass(org.springframework.amqp.core.Exchange.class);
        verify(rabbitAdmin).declareExchange(captor.capture());
        assertEquals("orders.exchange", captor.getValue().getName());
        assertEquals(DirectExchange.class, captor.getValue().getClass());
    }

    @Test
    void consultaTipoRealYDeclaraBinding() {
        when(managementClient.obtenerTipoExchange("orders.exchange")).thenReturn("topic");
        service.crearBinding(new BindingRequest("orders.exchange", "orders.queue", "pedido.*"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(rabbitAdmin).declareBinding(captor.capture());
        assertEquals("orders.queue", captor.getValue().getDestination());
        assertEquals("orders.exchange", captor.getValue().getExchange());
        assertEquals("pedido.*", captor.getValue().getRoutingKey());
    }

    @Test
    void rechazaBindingSinRoutingKeyParaExchangeDirect() {
        when(managementClient.obtenerTipoExchange("orders.exchange")).thenReturn("direct");
        assertThrows(IllegalArgumentException.class,
                () -> service.crearBinding(new BindingRequest("orders.exchange", "orders.queue", null)));
    }

    @Test
    void eliminaBindingLuegoDeConsultarExistenciaYTipo() {
        when(managementClient.obtenerTipoExchange("orders.exchange")).thenReturn("fanout");
        service.eliminarBinding(new BindingRequest("orders.exchange", "orders.queue", null));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(rabbitAdmin).removeBinding(captor.capture());
        assertEquals("orders.queue", captor.getValue().getDestination());
        assertEquals("orders.exchange", captor.getValue().getExchange());
    }
}
