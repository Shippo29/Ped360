package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminRequestValidationTest {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void crearValidador() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        validatorFactory.close();
    }

    @Test
    void rechazaNombresVaciosYConCaracteresInvalidos() {
        assertFalse(validator.validate(new QueueRequest("")).isEmpty());
        assertFalse(validator.validate(new QueueRequest("cola invalida")).isEmpty());
        assertFalse(validator.validate(new ExchangeRequest("ok", "invalid")).isEmpty());
    }

    @Test
    void aceptaBindingConRoutingKeyOpcional() {
        assertTrue(validator.validate(new BindingRequest("exchange", "queue", null)).isEmpty());
    }

    @Test
    void validaPoliticasDeRetencionYAltaDisponibilidad() {
        PolicyRequest policy = new PolicyRequest("retencion", "^cola\\..*\\.dlq$",
                "queues", 10, Map.of("message-ttl", 30000, "max-length", 100, "overflow", "drop-head"));
        assertTrue(validator.validate(policy).isEmpty());
        assertFalse(validator.validate(new PolicyRequest("invalida", "[", "queues", 0,
                Map.of("message-ttl", 0))).isEmpty());
        assertFalse(validator.validate(new PolicyRequest("invalida", ".*", "queues", 0,
                Map.of("ha-mode", "all", "otra-opcion", true))).isEmpty());
    }
}
