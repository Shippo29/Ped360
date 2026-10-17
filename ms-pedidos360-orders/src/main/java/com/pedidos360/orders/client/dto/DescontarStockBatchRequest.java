package com.pedidos360.orders.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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
