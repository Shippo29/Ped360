package com.pedidos360.orders.repository;

import com.pedidos360.orders.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteId(String clienteId);
}
