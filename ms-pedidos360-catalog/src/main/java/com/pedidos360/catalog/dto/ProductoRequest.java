package com.pedidos360.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequest {

    @NotBlank(message = "nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "precio no puede ser negativo")
    private BigDecimal precio;

    @NotNull(message = "stock es obligatorio")
    @Min(value = 0, message = "stock no puede ser negativo")
    private Integer stock;
}
