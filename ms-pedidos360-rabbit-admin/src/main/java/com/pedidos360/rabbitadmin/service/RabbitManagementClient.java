package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.dto.PolicyRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class RabbitManagementClient {

    private final List<RestClient> restClients;
    private final List<String> baseUrls;

    public RabbitManagementClient(
            @Value("${pedidos360.rabbit.management-urls}") List<String> baseUrls,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password) {
        this.baseUrls = List.copyOf(baseUrls);
        this.restClients = this.baseUrls.stream()
                .map(baseUrl -> RestClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                        .requestFactory(new SimpleClientHttpRequestFactory())
                        .build())
                .toList();
    }

    public String obtenerTipoExchange(String nombre) {
        try {
            ExchangeDetails exchange = conFailover(client -> client.get()
                    .uri(URI.create(baseUrl(client) + "/api/exchanges/%2F/" + nombre))
                    .retrieve().body(ExchangeDetails.class));
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
            conFailover(client -> client.get()
                    .uri(URI.create(baseUrl(client) + "/api/queues/%2F/" + nombre))
                    .retrieve().toBodilessEntity());
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("No existe la cola: " + nombre, ex);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarPoliticas() {
        List<Map<String, Object>> policies = conFailover(client -> client.get()
                .uri(URI.create(baseUrl(client) + "/api/policies/%2F"))
                .retrieve().body(List.class));
        if (policies == null) {
            throw new IllegalStateException("RabbitMQ no devolvió la lista de políticas");
        }
        return policies;
    }

    public void crearPolitica(PolicyRequest request) {
        Map<String, Object> body = Map.of(
                "pattern", request.pattern(),
                "definition", request.definition(),
                "apply-to", request.applyTo(),
                "priority", request.priority());
        conFailover(client -> client.put()
                .uri(URI.create(baseUrl(client) + "/api/policies/%2F/" + request.name()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().toBodilessEntity());
    }

    public void eliminarPolitica(String nombre) {
        try {
            conFailover(client -> client.delete()
                    .uri(URI.create(baseUrl(client) + "/api/policies/%2F/" + nombre))
                    .retrieve().toBodilessEntity());
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("No existe la política: " + nombre, ex);
        }
    }

    private <T> T conFailover(Function<RestClient, T> operation) {
        ResourceAccessException lastUnavailable = null;
        for (RestClient client : restClients) {
            try {
                return operation.apply(client);
            } catch (ResourceAccessException ex) {
                lastUnavailable = ex;
            } catch (HttpClientErrorException.NotFound ex) {
                throw ex;
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 422) {
                    throw new IllegalArgumentException(
                            "RabbitMQ rechazó la solicitud de administración (HTTP "
                                    + ex.getStatusCode().value() + ")", ex);
                }
                throw ex;
            }
        }
        throw new IllegalStateException("No se pudo conectar a ningún nodo de RabbitMQ Management",
                lastUnavailable);
    }

    private String baseUrl(RestClient client) {
        int index = restClients.indexOf(client);
        return baseUrls.get(index);
    }

    private record ExchangeDetails(String type) {
    }
}
