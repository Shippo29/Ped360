package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;
import java.util.regex.PatternSyntaxException;

public record PolicyRequest(
        @NotBlank(message = "El nombre de la política es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El nombre solo puede contener letras, números, punto, guion y guion bajo")
        String name,
        @NotBlank(message = "El patrón de la política es obligatorio")
        String pattern,
        @NotBlank(message = "El ámbito debe ser queues, exchanges o all")
        @Pattern(regexp = "^(queues|exchanges|all)$", message = "El ámbito debe ser queues, exchanges o all")
        String applyTo,
        @NotNull(message = "La prioridad es obligatoria")
        @Min(value = 0, message = "La prioridad no puede ser negativa")
        Integer priority,
        @NotEmpty(message = "La definición de la política no puede estar vacía")
        Map<String, Object> definition) {

    @AssertTrue(message = "El patrón de la política debe ser una expresión regular válida")
    public boolean isPatternValid() {
        if (pattern == null || pattern.isBlank()) {
            return true;
        }
        try {
            java.util.regex.Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    @AssertTrue(message = "Definición no válida; use valores positivos para TTL/max-length, overflow drop-head y ha-mode all/ha-sync-mode automatic")
    public boolean isDefinitionSupported() {
        if (definition == null || definition.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : definition.entrySet()) {
            Object value = entry.getValue();
            switch (entry.getKey()) {
                case "message-ttl", "max-length" -> {
                    if (!(value instanceof Number number)
                            || number.longValue() <= 0
                            || number.doubleValue() != number.longValue()) {
                        return false;
                    }
                }
                case "overflow" -> {
                    if (!"drop-head".equals(value)) {
                        return false;
                    }
                }
                case "ha-mode" -> {
                    if (!"all".equals(value)) {
                        return false;
                    }
                }
                case "ha-sync-mode" -> {
                    if (!"automatic".equals(value)) {
                        return false;
                    }
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }
}
