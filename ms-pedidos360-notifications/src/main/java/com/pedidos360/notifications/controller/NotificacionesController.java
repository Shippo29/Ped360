package com.pedidos360.notifications.controller;

import com.pedidos360.notifications.service.NotificacionTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificacionesController {

    private final NotificacionTrackerService trackerService;

    @GetMapping("/history")
    public List<NotificacionTrackerService.EventoRegistrado> historial() {
        return trackerService.listar();
    }
}
