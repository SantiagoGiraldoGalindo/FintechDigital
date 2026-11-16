package com.fintech.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void seededApiListsUsersWalletsGraphCyclesAndAnalytics() throws Exception {
        String usuariosJson = mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(6)))
                .andReturn().getResponse().getContentAsString();
        long primerId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(usuariosJson).get(0).get("id").asLong();
        mockMvc.perform(get("/api/billeteras").param("idUsuario", Long.toString(primerId)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/grafo/usuarios/vertices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
        mockMvc.perform(get("/api/grafo/usuarios/ciclos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());
        mockMvc.perform(get("/api/analitica")
                        .param("desde", "2020-01-01T00:00:00")
                        .param("hasta", "2030-01-01T00:00:00")
                        .param("k", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoTotal").exists())
                .andExpect(jsonPath("$.informe").exists());
    }

    @Test
    void validacion404YConflictoTienenFormatoJsonConsistente() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"correo\":\"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.nombre").exists());
        mockMvc.perform(get("/api/usuarios/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
        mockMvc.perform(post("/api/operaciones/retiro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoOrigen\":\"DEMO-ANA-A\",\"monto\":999999}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void creaBilleteraYOperacionYConsultaHistorial() throws Exception {
        long idUsuario = crearUsuario();
        mockMvc.perform(post("/api/billeteras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":" + idUsuario
                                + ",\"codigo\":\"API-TEST-WALLET\",\"nombre\":\"Test\","
                                + "\"tipo\":\"AHORRO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("API-TEST-WALLET"));
        mockMvc.perform(post("/api/operaciones/recarga").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoDestino\":\"API-TEST-WALLET\",\"monto\":25}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));
        mockMvc.perform(get("/api/historial").param("idUsuario", Long.toString(idUsuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/notificaciones/usuarios/" + idUsuario))
                .andExpect(status().isOk());
    }

    @Test
    void updateInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/api/usuarios/999999999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nadie\",\"correo\":\"nadie@example.com\",\"puntos\":0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private long crearUsuario() throws Exception {
        String respuesta = mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Mock User\",\"correo\":\"mock-api@example.com\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(respuesta).get("id").asLong();
    }
}
