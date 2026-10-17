package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QueueRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El nombre solo puede contener letras, números, punto, guion y guion bajo")
        String name) {
}
