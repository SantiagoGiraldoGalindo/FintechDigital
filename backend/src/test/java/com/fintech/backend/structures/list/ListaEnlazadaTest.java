package com.fintech.backend.structures.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ListaEnlazadaTest {

    @Test
    void agregaAlInicioYAlFinalYPermiteRecorrer() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("b");
        lista.agregarAlInicio("a");
        lista.agregar("c");

        assertIterableEquals(List.of("a", "b", "c"), lista);
        assertEquals(3, lista.size());
        assertEquals("a", lista.obtener(0));
        assertEquals("c", lista.obtener(2));
        assertTrue(lista.contiene("b"));
        assertFalse(lista.contiene("x"));
    }

    @Test
    void eliminaPorIndiceYValorIncluyendoExtremos() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregar(1);
        lista.agregar(2);
        lista.agregar(2);
        lista.agregar(3);

        assertEquals(1, lista.eliminar(0));
        assertTrue(lista.eliminar(Integer.valueOf(2)));
        assertEquals(3, lista.eliminar(1));
        assertFalse(lista.eliminar(Integer.valueOf(8)));
        assertEquals(1, lista.size());
        assertEquals(2, lista.obtener(0));
    }

    @Test
    void vaciaLimpiarYLimitesDeIndice() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        assertTrue(lista.estaVacia());
        assertEquals(0, lista.size());
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(0));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.eliminar(0));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));

        lista.agregar("único");
        assertEquals("único", lista.eliminar(0));
        assertTrue(lista.estaVacia());
        lista.agregar("otro");
        lista.limpiar();
        assertTrue(lista.estaVacia());
        assertFalse(lista.iterator().hasNext());
    }
}
