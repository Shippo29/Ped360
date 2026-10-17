package com.pedidos360.bff.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Un RestClient por microservicio real que hay detrás del BFF. Cada uno
 * apunta a su propia base URL (orders.service.url / catalog.service.url),
 * configurable por variable de entorno igual que en ms-pedidos360-orders.
 */
@Configuration
public class DownstreamClientsConfig {

    @Bean
    @Qualifier("ordersRestClient")
    public RestClient ordersRestClient(RestClient.Builder builder,
                                        @Value("${orders.service.url}") String ordersUrl) {
        return builder.baseUrl(ordersUrl).build();
    }

    @Bean
    @Qualifier("catalogRestClient")
    public RestClient catalogRestClient(RestClient.Builder builder,
                                         @Value("${catalog.service.url}") String catalogUrl) {
        return builder.baseUrl(catalogUrl).build();
    }
}
