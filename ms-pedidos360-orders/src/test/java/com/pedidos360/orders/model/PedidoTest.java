package com.pedidos360.orders.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PedidoTest {

    @Test
    void calculaElTotalComoPrecioUnitarioPorCantidad() {
        Pedido pedido = Pedido.builder().build();
        pedido.agregarItem(PedidoItem.builder()
                .productoId(1L)
                .nombreProducto("Producto")
                .cantidad(3)
                .precioUnitario(new BigDecimal("12.50"))
                .build());

        assertEquals(new BigDecimal("37.50"), pedido.calcularTotal());
    }

    @Test
    void devuelveCeroCuandoNoHayItems() {
        assertEquals(BigDecimal.ZERO, Pedido.builder().build().calcularTotal());
    }
}
