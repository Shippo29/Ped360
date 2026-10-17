package com.pedidos360.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearPedidoRequest {

    /**
     * Opcional. Solo tiene efecto si el actor autenticado es Operador o Admin:
     * permite registrar un pedido "por encargo" (ej. telefónico) a nombre del
     * cliente real indicado aquí. Si el actor es Cliente, este valor se
     * ignora siempre y el pedido queda a nombre del propio actor (nunca se
     * confía en un clienteId enviado por el cliente mismo).
     */
    private String clienteId;

    @NotEmpty(message = "el pedido debe tener al menos un producto")
    @Valid
    private List<ItemPedidoRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemPedidoRequest {

        @jakarta.validation.constraints.NotNull(message = "productoId es obligatorio")
        private Long productoId;

        @jakarta.validation.constraints.Min(value = 1, message = "la cantidad debe ser al menos 1")
        private int cantidad;
    }
}
