package com.pedidos360.catalog.repository;

import com.pedidos360.catalog.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
