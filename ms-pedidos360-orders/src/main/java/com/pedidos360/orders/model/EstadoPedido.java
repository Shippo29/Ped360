package com.pedidos360.orders.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum EstadoPedido {
    CREADO,
    ACEPTADO,
    EN_PREPARACION,
    DESPACHADO,
    ENTREGADO,
    CANCELADO;

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES_VALIDAS = Map.of(
            CREADO, EnumSet.of(ACEPTADO, CANCELADO),
            ACEPTADO, EnumSet.of(EN_PREPARACION, CANCELADO),
            EN_PREPARACION, EnumSet.of(DESPACHADO, CANCELADO),
            DESPACHADO, EnumSet.of(ENTREGADO),
            ENTREGADO, EnumSet.noneOf(EstadoPedido.class),
            CANCELADO, EnumSet.noneOf(EstadoPedido.class)
    );

    public boolean puedeTransicionarA(EstadoPedido destino) {
        return TRANSICIONES_VALIDAS.getOrDefault(this, Set.of()).contains(destino);
    }
}
