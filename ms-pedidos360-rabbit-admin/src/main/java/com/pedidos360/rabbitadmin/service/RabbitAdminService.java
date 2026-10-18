package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.dto.BindingRequest;
import com.pedidos360.rabbitadmin.dto.PolicyRequest;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.Set;

@Service
public class RabbitAdminService {

    private final AmqpAdmin rabbitAdmin;
    private final RabbitManagementClient managementClient;

    public RabbitAdminService(AmqpAdmin rabbitAdmin, RabbitManagementClient managementClient) {
        this.rabbitAdmin = rabbitAdmin;
        this.managementClient = managementClient;
    }

    public void crearCola(String nombre) {
        rabbitAdmin.declareQueue(new Queue(nombre, true));
    }

    public void eliminarCola(String nombre) {
        rabbitAdmin.deleteQueue(nombre);
    }

    public void crearExchange(String nombre, String tipo) {
        rabbitAdmin.declareExchange(construirExchange(nombre, tipo));
    }

    public void eliminarExchange(String nombre) {
        rabbitAdmin.deleteExchange(nombre);
    }

    public void crearBinding(BindingRequest request) {
        validarBinding(request);
        Binding binding = new Binding(
                request.queueName(),
                Binding.DestinationType.QUEUE,
                request.exchangeName(),
                routingKey(request),
                Map.of());
        rabbitAdmin.declareBinding(binding);
    }

    public void eliminarBinding(BindingRequest request) {
        validarBinding(request);
        Binding binding = new Binding(
                request.queueName(),
                Binding.DestinationType.QUEUE,
                request.exchangeName(),
                routingKey(request),
                Map.of());
        rabbitAdmin.removeBinding(binding);
    }

    public List<Map<String, Object>> listarPoliticas() {
        return managementClient.listarPoliticas();
    }

    public void crearPolitica(PolicyRequest request) {
        managementClient.crearPolitica(request);
    }

    public void eliminarPolitica(String nombre) {
        managementClient.eliminarPolitica(nombre);
    }

    private Exchange construirExchange(String nombre, String tipo) {
        return switch (tipo) {
            case "direct" -> new DirectExchange(nombre);
            case "topic" -> new TopicExchange(nombre);
            case "fanout" -> new FanoutExchange(nombre);
            case "headers" -> new HeadersExchange(nombre);
            default -> throw new IllegalArgumentException("Tipo de exchange no permitido: " + tipo);
        };
    }

    private void validarBinding(BindingRequest request) {
        managementClient.validarColaExiste(request.queueName());
        String exchangeType = managementClient.obtenerTipoExchange(request.exchangeName());
        if (!Set.of("direct", "topic", "fanout", "headers").contains(exchangeType)) {
            throw new IllegalArgumentException("Tipo de exchange no permitido: " + exchangeType);
        }
        if (("direct".equals(exchangeType) || "topic".equals(exchangeType))
                && (request.routingKey() == null || request.routingKey().isBlank())) {
            throw new IllegalArgumentException("routingKey es obligatoria para exchanges direct y topic");
        }
    }

    private String routingKey(BindingRequest request) {
        return request.routingKey() == null ? "" : request.routingKey();
    }
}
