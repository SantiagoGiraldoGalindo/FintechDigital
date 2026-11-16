package com.fintech.backend.structures.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class ColaTest {

    @Test
    void colaSinLimiteRespetaFifo() {
        Cola<String> cola = new Cola<>();
        cola.encolar("primero");
        cola.encolar("segundo");

        assertEquals("primero", cola.frente());
        assertEquals("primero", cola.desencolar());
        assertEquals("segundo", cola.desencolar());
        assertTrue(cola.estaVacia());
        assertEquals(0, cola.size());
    }

    @Test
    void colaAcotadaDescartaElElementoMasAntiguo() {
        Cola<Integer> cola = new Cola<>(2);
        cola.encolar(1);
        cola.encolar(2);
        cola.encolar(3);

        assertEquals(2, cola.size());
        assertEquals(2, cola.frente());
        assertEquals(2, cola.desencolar());
        assertEquals(3, cola.desencolar());
    }

    @Test
    void vaciaCapacidadUnoYCapacidadInvalida() {
        Cola<Integer> cola = new Cola<>();
        assertTrue(cola.estaVacia());
        assertThrows(NoSuchElementException.class, cola::desencolar);
        assertThrows(NoSuchElementException.class, cola::frente);

        Cola<Integer> capacidadUno = new Cola<>(1);
        capacidadUno.encolar(1);
        capacidadUno.encolar(2);
        assertEquals(1, capacidadUno.size());
        assertEquals(2, capacidadUno.desencolar());
        assertThrows(IllegalArgumentException.class, () -> new Cola<>(0));
        assertThrows(IllegalArgumentException.class, () -> new Cola<>(-1));
    }
}
