package com.pedidos360.bff.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

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

    @Bean
    @Qualifier("notificationsRestClient")
    public RestClient notificationsRestClient(RestClient.Builder builder,
                                               @Value("${notifications.service.url}") String notificationsUrl) {
        return builder.baseUrl(notificationsUrl).build();
    }
}
