package com.pedidos360.orders.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Línea de un pedido. Ya no tiene una relación JPA hacia Producto: Catálogo
 * vive en su propio servicio y su propia base de datos. Por eso se guarda
 * solo el id del producto más un "snapshot" de nombre y precio al momento
 * de crear el pedido (así el pedido no cambia si después editan el producto).
 */
@Entity
@Table(name = "pedido_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 150)
    private String nombreProducto;

    @Column(nullable = false)
    private Integer cantidad;

    /** Precio unitario "congelado" al momento de crear el pedido. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;
}
