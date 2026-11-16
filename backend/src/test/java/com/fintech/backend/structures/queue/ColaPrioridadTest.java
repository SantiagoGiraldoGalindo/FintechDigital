package com.fintech.backend.structures.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class ColaPrioridadTest {

    @Test
    void extraeEnOrdenDePrioridadYPermiteVerSinExtraer() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>(Integer::compareTo);
        cola.insertar(5);
        cola.insertar(1);
        cola.insertar(3);

        assertEquals(1, cola.verPrimero());
        assertEquals(3, cola.size());
        assertEquals(1, cola.extraer());
        assertEquals(3, cola.extraer());
        assertEquals(5, cola.extraer());
        assertEquals(0, cola.size());
    }

    @Test
    void monticuloCreceYSoportaOrdenInvertido() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>((a, b) -> b.compareTo(a));
        for (int i = 0; i < 25; i++) {
            cola.insertar(i);
        }

        for (int i = 24; i >= 0; i--) {
            assertEquals(i, cola.extraer());
        }
    }

    @Test
    void vaciaYElementosNull() {
        ColaPrioridad<Integer> cola = new ColaPrioridad<>(Integer::compareTo);
        assertThrows(NoSuchElementException.class, cola::extraer);
        assertThrows(NoSuchElementException.class, cola::verPrimero);
        assertThrows(NullPointerException.class, () -> cola.insertar(null));
        assertEquals(0, cola.size());
    }
}
