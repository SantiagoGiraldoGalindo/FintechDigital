package com.fintech.backend.structures.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class ArbolAVLTest {

    @Test
    void insertaMilClavesOrdenadasYConservaBalanceAVL() throws ReflectiveOperationException {
        ArbolAVL<Integer, Integer> arbol = new ArbolAVL<>();
        for (int i = 0; i < 2_000; i++) {
            arbol.insertar(i, i);
        }

        assertEquals(2_000, arbol.size());
        assertEquals(0, arbol.minimo());
        assertEquals(1_999, arbol.maximo());
        assertTrue(revisarBalance(arbol) <= 16, "La altura debe crecer logarítmicamente");
    }

    @Test
    void buscaRangoInclusivoYRecorridosOrdenados() {
        ArbolAVL<Integer, String> arbol = new ArbolAVL<>();
        for (int clave : List.of(8, 3, 10, 1, 6, 14, 4, 7, 13)) {
            arbol.insertar(clave, "v" + clave);
        }

        assertEquals(List.of("v4", "v6", "v7", "v8", "v10"),
                comoLista(arbol.rango(4, 10)));
        assertEquals(List.of("v1", "v3", "v4", "v6", "v7", "v8", "v10", "v13", "v14"),
                comoLista(arbol.recorridoInOrden()));
        assertEquals(List.of("v14", "v13", "v10", "v8", "v7", "v6", "v4", "v3", "v1"),
                comoLista(arbol.recorridoDescendente()));
        assertEquals(List.of(), comoLista(arbol.rango(20, 30)));
        assertThrows(IllegalArgumentException.class, () -> arbol.rango(5, 4));
    }

    @Test
    void claveDuplicadaActualizaValorYEliminacionMantieneBalance() throws ReflectiveOperationException {
        ArbolAVL<Integer, String> arbol = new ArbolAVL<>();
        for (int i = 0; i < 100; i++) {
            arbol.insertar(i, "anterior-" + i);
        }
        arbol.insertar(50, "actualizado");
        assertEquals(100, arbol.size());
        assertEquals("actualizado", arbol.buscar(50));
        assertEquals("anterior-49", arbol.eliminar(49));
        assertNull(arbol.eliminar(500));
        assertEquals(99, arbol.size());
        assertNull(arbol.buscar(49));
        assertTrue(revisarBalance(arbol) <= 8);
    }

    @Test
    void arbolVacioYClavesNull() {
        ArbolAVL<Integer, String> arbol = new ArbolAVL<>();
        assertEquals(0, arbol.size());
        assertNull(arbol.buscar(1));
        assertNull(arbol.eliminar(1));
        assertThrows(NoSuchElementException.class, arbol::minimo);
        assertThrows(NoSuchElementException.class, arbol::maximo);
        assertThrows(NullPointerException.class, () -> arbol.insertar(null, "x"));
        assertThrows(NullPointerException.class, () -> arbol.insertar(1, null));
    }

    private static <T> List<T> comoLista(Iterable<T> iterable) {
        List<T> resultado = new ArrayList<>();
        iterable.forEach(resultado::add);
        return resultado;
    }

    private static int revisarBalance(ArbolAVL<?, ?> arbol) throws ReflectiveOperationException {
        Field raiz = ArbolAVL.class.getDeclaredField("raiz");
        raiz.setAccessible(true);
        return revisarNodo(raiz.get(arbol));
    }

    private static int revisarNodo(Object nodo) throws ReflectiveOperationException {
        if (nodo == null) {
            return 0;
        }
        Class<?> tipo = nodo.getClass();
        Field izquierdo = tipo.getDeclaredField("izquierdo");
        Field derecho = tipo.getDeclaredField("derecho");
        Field altura = tipo.getDeclaredField("altura");
        izquierdo.setAccessible(true);
        derecho.setAccessible(true);
        altura.setAccessible(true);

        int alturaIzquierda = revisarNodo(izquierdo.get(nodo));
        int alturaDerecha = revisarNodo(derecho.get(nodo));
        assertEquals(alturaIzquierda + 1 > alturaDerecha
                        ? alturaIzquierda + 1 : alturaDerecha + 1,
                altura.getInt(nodo));
        assertTrue(Math.abs(alturaIzquierda - alturaDerecha) <= 1,
                "Cada nodo debe cumplir la condición AVL");
        return altura.getInt(nodo);
    }
}
