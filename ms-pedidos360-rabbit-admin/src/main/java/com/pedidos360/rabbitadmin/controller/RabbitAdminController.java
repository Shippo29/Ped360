package com.pedidos360.rabbitadmin.controller;

import com.pedidos360.rabbitadmin.dto.BindingRequest;
import com.pedidos360.rabbitadmin.dto.ExchangeRequest;
import com.pedidos360.rabbitadmin.dto.QueueRequest;
import com.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Validated
public class RabbitAdminController {

    private static final String NAME_PATTERN = "^[A-Za-z0-9._-]+$";
    private static final String INVALID_NAME_MESSAGE =
            "El nombre solo puede contener letras, números, punto, guion y guion bajo";
    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping("/queues")
    public ResponseEntity<Void> crearCola(@Valid @RequestBody QueueRequest request) {
        rabbitAdminService.crearCola(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(
            @PathVariable
            @NotBlank(message = "El nombre de la cola es obligatorio")
            @Pattern(regexp = NAME_PATTERN, message = INVALID_NAME_MESSAGE) String name) {
        rabbitAdminService.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Void> crearExchange(@Valid @RequestBody ExchangeRequest request) {
        rabbitAdminService.crearExchange(request.name(), request.type());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(
            @PathVariable
            @NotBlank(message = "El nombre del exchange es obligatorio")
            @Pattern(regexp = NAME_PATTERN, message = INVALID_NAME_MESSAGE) String name) {
        rabbitAdminService.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Void> crearBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.crearBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }
}
