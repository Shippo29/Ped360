package com.pedidos360.orders.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador del actor dueño del pedido (claim "oid"/"sub" del JWT de
     * Entra ID). Se usa para filtrar "mis pedidos" cuando el actor es CLIENTE.
     */
    @Column(nullable = false, length = 100)
    private String clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.CREADO;

    @Column(nullable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    private Instant fechaActualizacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<PedidoItem> items = new ArrayList<>();

    public void agregarItem(PedidoItem item) {
        item.setPedido(this);
        this.items.add(item);
    }
}
