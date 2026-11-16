package com.fintech.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

/** Verifica la respuesta HTTP real sin depender de MySQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate cliente;

    @Test
    void endpointHealthRespondeUp() {
        ResponseEntity<Map> respuesta = cliente.getForEntity(
                "http://localhost:" + puerto + "/api/health",
                Map.class);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("UP", respuesta.getBody().get("status"));
        assertNotNull(respuesta.getBody().get("timestamp"));
    }

    @Test
    void endpointBenchmarkEntregaResultadosParaLosTresTamanos() {
        ResponseEntity<Map> respuesta = cliente.getForEntity(
                "http://localhost:" + puerto + "/api/benchmark", Map.class);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(15, ((java.util.List<?>) respuesta.getBody().get("mediciones")).size());
        assertNotNull(respuesta.getBody().get("fechaEjecucion"));
    }
}
