package com.pedidos360.notifications.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
class NotificacionesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void historyRespondeConListaVaciaSinAutenticacionNiBaseDeDatos() throws Exception {
        mockMvc.perform(get("/api/notifications/history"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
