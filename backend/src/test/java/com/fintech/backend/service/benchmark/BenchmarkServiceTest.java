package com.fintech.backend.service.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BenchmarkServiceTest {

    @Test
    void ejecutaLosCuatroCasosParaLosTresTamanos() {
        BenchmarkService.BenchmarkReport resultado = new BenchmarkService().ejecutar();

        assertEquals(15, resultado.mediciones().size());
        assertTrue(resultado.mediciones().stream().allMatch(m -> m.nanosPropia() >= 0
                && m.nanosReferencia() >= 0 && m.operacionesMedidas() > 0));
        assertEquals(1_000, resultado.mediciones().get(0).n());
        assertEquals(100_000, resultado.mediciones().get(14).n());
    }
}
