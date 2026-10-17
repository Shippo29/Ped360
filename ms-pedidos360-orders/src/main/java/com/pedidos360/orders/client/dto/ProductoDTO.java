package com.pedidos360.orders.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Réplica del contrato JSON expuesto por ms-pedidos360-catalog
 * (GET /api/catalog/products/{id}). No es una entidad JPA: Orders no es
 * dueño de estos datos, solo los consume.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private Boolean activo;
}
