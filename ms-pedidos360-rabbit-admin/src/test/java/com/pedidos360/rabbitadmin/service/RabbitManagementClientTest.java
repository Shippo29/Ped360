package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.dto.PolicyRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitManagementClientTest {

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void iniciarServidor() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath() + " "
                    + exchange.getRequestHeaders().getFirst("Content-Type") + " " + body);
            byte[] response = exchange.getRequestMethod().equals("GET")
                    ? "[]".getBytes(StandardCharsets.UTF_8)
                    : new byte[0];
            int status = exchange.getRequestMethod().equals("GET") ? 200
                    : exchange.getRequestMethod().equals("PUT") ? 201 : 204;
            if (exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
            }
            exchange.sendResponseHeaders(status, response.length == 0 ? -1 : response.length);
            if (response.length > 0) {
                exchange.getResponseBody().write(response);
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void detenerServidor() {
        server.stop(0);
    }

    @Test
    void usaElSegundoNodoParaGestionarPoliticasCuandoElPrimeroNoEstaDisponible() {
        String secondaryUrl = "http://localhost:" + server.getAddress().getPort();
        RabbitManagementClient client = new RabbitManagementClient(
                List.of("http://localhost:1", secondaryUrl), "usuario", "password");
        PolicyRequest policy = new PolicyRequest("retencion", "^cola\\.dlq$",
                "queues", 10, Map.of("message-ttl", 30000));

        assertTrue(client.listarPoliticas().isEmpty());
        client.crearPolitica(policy);
        client.eliminarPolitica("retencion");

        assertEquals(3, requests.size());
        assertTrue(requests.get(0).startsWith("GET /api/policies/%2F "));
        assertTrue(requests.get(1).startsWith("PUT /api/policies/%2F/retencion application/json "));
        assertTrue(requests.get(1).contains("\"apply-to\":\"queues\""));
        assertTrue(requests.get(2).startsWith("DELETE /api/policies/%2F/retencion "));
    }
}
