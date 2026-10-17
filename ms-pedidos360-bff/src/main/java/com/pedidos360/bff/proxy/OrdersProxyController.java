package com.pedidos360.bff.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Todo lo que el frontend manda a /api/orders/** se reenvía tal cual a
 * ms-pedidos360-orders (ver ProxyService). No se duplican DTOs ni reglas de
 * negocio acá: el BFF es solo el punto de entrada único para el frontend.
 */
@RestController
public class OrdersProxyController {

    private final ProxyService proxyService;
    private final RestClient ordersRestClient;

    public OrdersProxyController(ProxyService proxyService,
                                  @Qualifier("ordersRestClient") RestClient ordersRestClient) {
        this.proxyService = proxyService;
        this.ordersRestClient = ordersRestClient;
    }

    @RequestMapping("/api/orders/**")
    public ResponseEntity<byte[]> proxyOrders(HttpServletRequest request) {
        return proxyService.reenviar(ordersRestClient, request);
    }
}
