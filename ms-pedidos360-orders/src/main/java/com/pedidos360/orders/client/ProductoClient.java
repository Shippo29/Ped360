package com.pedidos360.orders.client;

import com.pedidos360.orders.client.dto.DescontarStockBatchRequest;
import com.pedidos360.orders.client.dto.DescontarStockRequest;
import com.pedidos360.orders.client.dto.ProductoDTO;
import com.pedidos360.orders.exception.RecursoNoEncontradoException;
import com.pedidos360.orders.exception.StockInsuficienteException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ProductoClient {

    private final RestClient restClient;

    public ProductoClient(RestClient.Builder builder, @Value("${catalog.service.url}") String catalogUrl) {
        this.restClient = builder.baseUrl(catalogUrl).build();
    }

    public ProductoDTO obtenerProducto(Long id) {
        try {
            return restClient.get()
                    .uri("/api/catalog/products/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDelActorActual())
                    .retrieve()
                    .body(ProductoDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RecursoNoEncontradoException("Producto no encontrado en catálogo: id=" + id);
        }
    }

    public void descontarStock(Long id, int cantidad) {
        try {
            restClient.put()
                    .uri("/api/catalog/products/{id}/stock/decrement", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDelActorActual())
                    .body(new DescontarStockRequest(cantidad))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new StockInsuficienteException("Stock insuficiente para el producto id=" + id);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RecursoNoEncontradoException("Producto no encontrado en catálogo: id=" + id);
        }
    }

    public void descontarStockBatch(List<DescontarStockBatchRequest.ItemDescontar> items) {
        try {
            restClient.put()
                    .uri("/api/catalog/products/stock/decrement-batch")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDelActorActual())
                    .body(new DescontarStockBatchRequest(items))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new StockInsuficienteException("Stock insuficiente para uno o más productos del pedido");
        } catch (HttpClientErrorException.NotFound e) {
            throw new RecursoNoEncontradoException("Uno o más productos del pedido no existen en catálogo");
        }
    }

    private String tokenDelActorActual() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getTokenValue();
    }
}
