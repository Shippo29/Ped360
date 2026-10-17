package com.pedidos360.orders.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Espejo del DTO equivalente en ms-pedidos360-catalog: permite pedir el
 * descuento de stock de todos los ítems de un pedido en una sola llamada,
 * para que Catálogo lo aplique como una única transacción atómica.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescontarStockBatchRequest {

    private List<ItemDescontar> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDescontar {
        private Long productoId;
        private Integer cantidad;
    }
}
