package com.pedidos360.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Descuento de stock para TODOS los ítems de un pedido en una sola llamada,
 * para que Catálogo pueda aplicarlos como una única transacción local
 * (todo o nada) en vez de que Orders los pida uno por uno. Ver
 * ProductoService.descontarStockBatch().
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescontarStockBatchRequest {

    @NotEmpty(message = "el batch debe tener al menos un item")
    @Valid
    private List<ItemDescontar> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDescontar {

        @NotNull(message = "productoId es obligatorio")
        private Long productoId;

        @NotNull(message = "cantidad es obligatoria")
        @Min(value = 1, message = "cantidad debe ser al menos 1")
        private Integer cantidad;
    }
}
