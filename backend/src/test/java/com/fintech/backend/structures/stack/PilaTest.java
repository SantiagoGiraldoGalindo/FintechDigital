package com.fintech.backend.structures.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class PilaTest {

    @Test
    void pushPopYPeekRespetanLifo() {
        Pila<String> pila = new Pila<>();
        pila.push("primero");
        pila.push("segundo");

        assertEquals("segundo", pila.peek());
        assertEquals("segundo", pila.pop());
        assertEquals("primero", pila.pop());
        assertTrue(pila.estaVacia());
        assertEquals(0, pila.size());
    }

    @Test
    void vaciaYUltimoElementoLanzanExcepcionClara() {
        Pila<Integer> pila = new Pila<>();
        assertTrue(pila.estaVacia());
        assertEquals(0, pila.size());
        assertThrows(NoSuchElementException.class, pila::pop);
        assertThrows(NoSuchElementException.class, pila::peek);

        pila.push(7);
        assertEquals(7, pila.pop());
        assertThrows(NoSuchElementException.class, pila::peek);
    }
}
