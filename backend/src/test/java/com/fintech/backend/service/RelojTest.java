package com.fintech.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RelojTest {

    @Test
    void permiteFijarYAvanzarElTiempo() {
        Reloj reloj = new Reloj();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 12, 20, 0);

        reloj.fijar(inicio);
        reloj.avanzar(Duration.ofHours(2));

        assertEquals(inicio.plusHours(2), reloj.ahora());
    }
}
