package com.pedidos360.bff.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Todo lo que el frontend manda a /api/catalog/** se reenvía tal cual a
 * ms-pedidos360-catalog (ver ProxyService).
 */
@RestController
public class CatalogProxyController {

    private final ProxyService proxyService;
    private final RestClient catalogRestClient;

    public CatalogProxyController(ProxyService proxyService,
                                   @Qualifier("catalogRestClient") RestClient catalogRestClient) {
        this.proxyService = proxyService;
        this.catalogRestClient = catalogRestClient;
    }

    @RequestMapping("/api/catalog/**")
    public ResponseEntity<byte[]> proxyCatalog(HttpServletRequest request) {
        return proxyService.reenviar(catalogRestClient, request);
    }
}
