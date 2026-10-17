package com.pedidos360.rabbitadmin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Component
public class RabbitManagementClient {

    private final RestClient restClient;
    private final String baseUrl;

    public RabbitManagementClient(
            @Value("${pedidos360.rabbit.management-url}") String baseUrl,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
    }

    public String obtenerTipoExchange(String nombre) {
        try {
            ExchangeDetails exchange = restClient.get()
                    .uri(URI.create(baseUrl + "/api/exchanges/%2F/" + nombre))
                    .retrieve()
                    .body(ExchangeDetails.class);
            if (exchange == null || exchange.type() == null) {
                throw new IllegalStateException("RabbitMQ no devolvió el tipo del exchange " + nombre);
            }
            return exchange.type();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("No existe el exchange: " + nombre, ex);
        }
    }

    public void validarColaExiste(String nombre) {
        try {
            restClient.get()
                    .uri(URI.create(baseUrl + "/api/queues/%2F/" + nombre))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("No existe la cola: " + nombre, ex);
        }
    }

    private record ExchangeDetails(String type) {
    }
}
