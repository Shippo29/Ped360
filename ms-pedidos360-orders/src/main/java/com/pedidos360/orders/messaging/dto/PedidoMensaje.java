package com.pedidos360.orders.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoMensaje {
    private Long pedidoId;
    private String clienteId;
    private String estado;
    private BigDecimal total;
    private Instant fecha;
    private String mensaje;
    private boolean forzarFalloParaDlq;
    private List<ItemMensaje> items;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemMensaje {
        private Long productoId;
        private String nombreProducto;
        private Integer cantidad;
        private BigDecimal precioUnitario;
    }
}
