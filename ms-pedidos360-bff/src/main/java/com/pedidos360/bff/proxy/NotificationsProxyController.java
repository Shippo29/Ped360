package com.pedidos360.bff.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class NotificationsProxyController {

    private final ProxyService proxyService;
    private final RestClient notificationsRestClient;

    public NotificationsProxyController(
            ProxyService proxyService,
            @Qualifier("notificationsRestClient") RestClient notificationsRestClient) {
        this.proxyService = proxyService;
        this.notificationsRestClient = notificationsRestClient;
    }

    @RequestMapping("/api/notifications/**")
    public ResponseEntity<byte[]> proxyNotifications(HttpServletRequest request) {
        return proxyService.reenviar(notificationsRestClient, request);
    }
}
