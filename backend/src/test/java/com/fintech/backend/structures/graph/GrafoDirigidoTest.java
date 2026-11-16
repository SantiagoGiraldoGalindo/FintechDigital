package com.fintech.backend.structures.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class GrafoDirigidoTest {

    @Test
    void grafoSinCiclosPermiteRecorridosGradosYCamino() {
        GrafoDirigido<String> grafo = new GrafoDirigido<>();
        grafo.agregarVertice("aislado");
        grafo.agregarArista("A", "B", 2.5);
        grafo.agregarArista("A", "C", 1.0);
        grafo.agregarArista("B", "D", 3.0);
        grafo.agregarArista("C", "D", 4.0);

        assertEquals(List.of("B", "C"), comoLista(grafo.vecinos("A")));
        assertEquals(2, grafo.gradoSalida("A"));
        assertEquals(2, grafo.gradoEntrada("D"));
        assertEquals(0, grafo.gradoEntrada("aislado"));
        assertEquals(List.of("A", "B", "C", "D"), comoLista(grafo.BFS("A")));
        assertEquals(List.of("A", "B", "D", "C"), comoLista(grafo.DFS("A")));
        assertTrue(grafo.existeCamino("A", "D"));
        assertFalse(grafo.existeCamino("D", "A"));
        assertEquals(0, grafo.detectarCiclos().size());
        assertEquals(5, grafo.cantidadVertices());
        assertEquals(4, grafo.cantidadAristas());
    }

    @Test
    void acumulaAristasYOrdenaFrecuencias() {
        GrafoDirigido<String> grafo = new GrafoDirigido<>();
        grafo.agregarArista("A", "B", new BigDecimal("10.25"));
        grafo.agregarArista("A", "B", new BigDecimal("2.75"));
        grafo.agregarArista("B", "C", 5.0);
        grafo.agregarArista("C", "A", 1.0);

        GrafoDirigido.Arista<String> masFrecuente = grafo.aristasMasFrecuentes(1).obtener(0);
        assertEquals("A", masFrecuente.getOrigen());
        assertEquals("B", masFrecuente.getDestino());
        assertEquals(2, masFrecuente.getVeces());
        assertEquals(new BigDecimal("13.00"), masFrecuente.getMontoTotal());
        assertEquals(1, grafo.rutasFrecuentes(2).size());
        assertEquals(3, grafo.aristasMasFrecuentes(10).size());
        assertEquals(0, grafo.aristasMasFrecuentes(0).size());
        assertThrows(IllegalArgumentException.class, () -> grafo.aristasMasFrecuentes(-1));
        assertThrows(IllegalArgumentException.class, () -> grafo.rutasFrecuentes(0));
    }

    @Test
    void detectaCicloDirigidoYAutoCicloEnComponenteDesconectado() {
        GrafoDirigido<Integer> grafo = new GrafoDirigido<>();
        grafo.agregarArista(1, 2, 1);
        grafo.agregarArista(2, 3, 1);
        grafo.agregarArista(3, 1, 1);
        grafo.agregarVertice(4);
        grafo.agregarArista(5, 5, 1);

        List<List<Integer>> ciclos = new ArrayList<>();
        for (var ciclo : grafo.detectarCiclos()) {
            ciclos.add(comoLista(ciclo));
        }
        assertTrue(ciclos.stream().anyMatch(ciclo -> esCiclo(ciclo, 1, 2, 3)));
        assertTrue(ciclos.contains(List.of(5, 5)));
        assertEquals(2, ciclos.size());
        assertFalse(grafo.existeCamino(4, 1));
        assertEquals(List.of(4), comoLista(grafo.BFS(4)));
        assertEquals(List.of(4), comoLista(grafo.DFS(4)));
    }

    @Test
    void rechazaConsultasDeVerticesDesconocidosYNull() {
        GrafoDirigido<String> grafo = new GrafoDirigido<>();
        grafo.agregarVertice("A");
        assertThrows(NoSuchElementException.class, () -> grafo.vecinos("B"));
        assertThrows(NoSuchElementException.class, () -> grafo.gradoEntrada("B"));
        assertThrows(NoSuchElementException.class, () -> grafo.BFS("B"));
        assertThrows(NullPointerException.class, () -> grafo.agregarVertice(null));
        assertThrows(NullPointerException.class, () -> grafo.agregarArista("A", "B", null));
    }

    private static <T> List<T> comoLista(Iterable<T> iterable) {
        List<T> resultado = new ArrayList<>();
        iterable.forEach(resultado::add);
        return resultado;
    }

    private static boolean esCiclo(List<Integer> ciclo, int... vertices) {
        if (ciclo.size() != vertices.length + 1
                || !ciclo.get(0).equals(ciclo.get(ciclo.size() - 1))) {
            return false;
        }
        HashSet<Integer> unicos = new HashSet<>(ciclo.subList(0, ciclo.size() - 1));
        if (unicos.size() != vertices.length) {
            return false;
        }
        for (int vertice : vertices) {
            if (!unicos.contains(vertice)) {
                return false;
            }
        }
        return true;
    }
}
