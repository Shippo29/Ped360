package com.pedidos360.notifications.service;

import com.pedidos360.notifications.messaging.dto.PedidoMensaje;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;

@Service
public class NotificacionTrackerService {

    private static final int MAX_EVENTOS = 500;
    private final ArrayDeque<EventoRegistrado> eventos = new ArrayDeque<>();

    public synchronized void registrar(String tipo, String detalle, PedidoMensaje mensaje) {
        if (eventos.size() == MAX_EVENTOS) {
            eventos.removeFirst();
        }
        eventos.addLast(new EventoRegistrado(
                tipo, detalle, Instant.now(), mensaje == null ? null : mensaje.getPedidoId()));
    }

    public synchronized List<EventoRegistrado> listar() {
        return List.copyOf(eventos);
    }

    public record EventoRegistrado(String tipo, String detalle, Instant fecha, Long pedidoId) {
    }
}
