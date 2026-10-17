package com.pedidos360.orders.controller;

import com.pedidos360.orders.dto.CambiarEstadoRequest;
import com.pedidos360.orders.dto.CrearPedidoRequest;
import com.pedidos360.orders.dto.PedidoResponse;
import com.pedidos360.orders.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    // Cliente crea sus propios pedidos; Operador/Admin también pueden generarlos
    // (p.ej. pedido tomado por teléfono, o supervisión).
    @PostMapping
    @PreAuthorize("hasAnyRole('Cliente', 'Operador', 'Admin')")
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest request) {
        PedidoResponse creado = pedidoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Listado filtrado según el actor: Cliente ve solo los suyos (filtrado en el service).
    @GetMapping
    @PreAuthorize("hasAnyRole('Cliente', 'Operador', 'Admin')")
    public List<PedidoResponse> listar() {
        return pedidoService.listarSegunActor();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Cliente', 'Operador', 'Admin')")
    public PedidoResponse detalle(@PathVariable Long id) {
        return pedidoService.obtenerDetalle(id);
    }

    // Solo Operador/Admin pueden avanzar el estado operacional del pedido.
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Operador', 'Admin')")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return pedidoService.cambiarEstado(id, request.getNuevoEstado());
    }
}
