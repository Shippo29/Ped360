package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BindingRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El nombre del exchange solo puede contener letras, números, punto, guion y guion bajo")
        String exchangeName,
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El nombre de la cola solo puede contener letras, números, punto, guion y guion bajo")
        String queueName,
        String routingKey) {
}
