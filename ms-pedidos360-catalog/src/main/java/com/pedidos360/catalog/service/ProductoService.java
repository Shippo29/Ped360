package com.pedidos360.catalog.service;

import com.pedidos360.catalog.dto.DescontarStockBatchRequest;
import com.pedidos360.catalog.dto.ProductoRequest;
import com.pedidos360.catalog.dto.ProductoResponse;
import com.pedidos360.catalog.exception.RecursoNoEncontradoException;
import com.pedidos360.catalog.exception.StockInsuficienteException;
import com.pedidos360.catalog.model.Producto;
import com.pedidos360.catalog.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream()
                .map(ProductoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.desde(buscarPorId(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = Producto.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .stock(request.getStock())
                .activo(true)
                .build();
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse editar(Long id, ProductoRequest request) {
        Producto producto = buscarPorId(id);
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    /**
     * Descuento atómico de stock, invocado por ms-pedidos360-orders cuando un
     * pedido pasa a ACEPTADO. Vive acá (y no en Orders) porque el stock es
     * responsabilidad exclusiva de este servicio ahora que están separados.
     */
    @Transactional
    public ProductoResponse descontarStock(Long id, int cantidad) {
        Producto producto = buscarPorId(id);
        if (cantidad > producto.getStock()) {
            throw new StockInsuficienteException(
                    "Stock insuficiente para '" + producto.getNombre() + "': disponible="
                            + producto.getStock() + ", solicitado=" + cantidad);
        }
        producto.descontarStock(cantidad);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    /**
     * Descuenta el stock de TODOS los ítems de un pedido en una única
     * transacción local de este servicio: si cualquier ítem no tiene stock
     * suficiente (o no existe), se lanza la excepción ANTES de guardar nada
     * y Spring hace rollback de los descuentos ya aplicados en este mismo
     * método (todo o nada).
     *
     * Esto resuelve el problema de consistencia que existía al descontar
     * ítem por ítem con llamadas HTTP separadas desde Orders: con ese
     * enfoque, si el ítem 1 se descontaba y el ítem 2 fallaba por falta de
     * stock, el descuento del ítem 1 quedaba aplicado en la base de datos de
     * Catálogo aunque Orders revirtiera el cambio de estado del pedido en su
     * propia base de datos (dos servicios, dos transacciones distintas).
     * Ahora toda la validación y el descuento ocurren dentro de la misma
     * transacción de Catálogo, así que o se aplican todos los ítems o no se
     * aplica ninguno.
     */
    @Transactional
    public List<ProductoResponse> descontarStockBatch(DescontarStockBatchRequest request) {
        List<Producto> productos = request.getItems().stream()
                .map(item -> {
                    Producto producto = buscarPorId(item.getProductoId());
                    if (item.getCantidad() > producto.getStock()) {
                        throw new StockInsuficienteException(
                                "Stock insuficiente para '" + producto.getNombre() + "': disponible="
                                        + producto.getStock() + ", solicitado=" + item.getCantidad());
                    }
                    return producto;
                })
                .toList();

        // Validación completa (arriba) antes de modificar nada: así, si un
        // ítem intermedio falla, ningún ítem anterior llega a descontarse.
        for (int i = 0; i < productos.size(); i++) {
            productos.get(i).descontarStock(request.getItems().get(i).getCantidad());
        }

        return productoRepository.saveAll(productos).stream()
                .map(ProductoResponse::desde)
                .toList();
    }

    private Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: id=" + id));
    }
}
