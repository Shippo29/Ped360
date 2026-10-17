package com.pedidos360.orders.dto;

import com.pedidos360.orders.model.EstadoPedido;
import com.pedidos360.orders.model.Pedido;
import com.pedidos360.orders.model.PedidoItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PedidoResponse {

    private Long id;
    private String clienteId;
    private EstadoPedido estado;
    private Instant fechaCreacion;
    private Instant fechaActualizacion;
    private List<ItemPedidoResponse> items;
    private BigDecimal total;

    public static PedidoResponse desde(Pedido pedido) {
        List<ItemPedidoResponse> items = pedido.getItems().stream()
                .map(ItemPedidoResponse::desde)
                .toList();

        return PedidoResponse.builder()
                .id(pedido.getId())
                .clienteId(pedido.getClienteId())
                .estado(pedido.getEstado())
                .fechaCreacion(pedido.getFechaCreacion())
                .fechaActualizacion(pedido.getFechaActualizacion())
                .items(items)
                .total(pedido.calcularTotal())
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemPedidoResponse {
        private Long productoId;
        private String nombreProducto;
        private int cantidad;
        private BigDecimal precioUnitario;

        public static ItemPedidoResponse desde(PedidoItem item) {
            return ItemPedidoResponse.builder()
                    .productoId(item.getProductoId())
                    .nombreProducto(item.getNombreProducto())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    .build();
        }
    }
}
