package com.pedidos360.catalog.controller;

import com.pedidos360.catalog.dto.DescontarStockBatchRequest;
import com.pedidos360.catalog.dto.DescontarStockRequest;
import com.pedidos360.catalog.dto.ProductoRequest;
import com.pedidos360.catalog.dto.ProductoResponse;
import com.pedidos360.catalog.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    // Consulta de catálogo: cualquier actor autenticado.
    @GetMapping
    @PreAuthorize("hasAnyRole('Cliente', 'Operador', 'Admin')")
    public List<ProductoResponse> listar() {
        return productoService.listar();
    }

    // Usado también por ms-pedidos360-orders para obtener precio/nombre al crear un pedido.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Cliente', 'Operador', 'Admin')")
    public ProductoResponse detalle(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = productoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ProductoResponse editar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.editar(id, request);
    }

    // Endpoint interno: solo lo invoca ms-pedidos360-orders al aceptar un pedido
    // (reenviando el JWT del Operador/Admin que hizo el cambio de estado).
    @PutMapping("/{id}/stock/decrement")
    @PreAuthorize("hasAnyRole('Operador', 'Admin')")
    public ProductoResponse descontarStock(@PathVariable Long id, @Valid @RequestBody DescontarStockRequest request) {
        return productoService.descontarStock(id, request.getCantidad());
    }

    // Endpoint interno: lo invoca ms-pedidos360-orders al aceptar un pedido,
    // enviando TODOS los ítems en una sola llamada para que el descuento sea
    // atómico (todo o nada) dentro de una única transacción de este servicio.
    @PutMapping("/stock/decrement-batch")
    @PreAuthorize("hasAnyRole('Operador', 'Admin')")
    public List<ProductoResponse> descontarStockBatch(@Valid @RequestBody DescontarStockBatchRequest request) {
        return productoService.descontarStockBatch(request);
    }
}
