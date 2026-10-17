package com.pedidos360.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescontarStockRequest {

    @NotNull(message = "cantidad es obligatoria")
    @Min(value = 1, message = "cantidad debe ser al menos 1")
    private Integer cantidad;
}
