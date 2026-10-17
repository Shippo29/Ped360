package com.pedidos360.orders.dto;

import com.pedidos360.orders.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoRequest {

    @NotNull(message = "nuevoEstado es obligatorio")
    private EstadoPedido nuevoEstado;
}
