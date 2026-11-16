package com.fintech.backend.structures.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

class TablaHashTest {

    @Test
    void colisionesSeResuelvenConEncadenamientoYSeEliminanBien() {
        TablaHash<ClaveColision, String> tabla = new TablaHash<>();
        ClaveColision primera = new ClaveColision(1);
        ClaveColision segunda = new ClaveColision(2);
        ClaveColision tercera = new ClaveColision(3);

        tabla.poner(primera, "uno");
        tabla.poner(segunda, "dos");
        tabla.poner(tercera, "tres");
        assertEquals("dos", tabla.obtener(new ClaveColision(2)));
        assertEquals("dos", tabla.eliminar(segunda));
        assertFalse(tabla.contieneClave(segunda));
        assertEquals("uno", tabla.obtener(primera));
        assertEquals("tres", tabla.obtener(tercera));
        assertEquals(2, tabla.size());
    }

    @Test
    void redimensionaManteniendoLasAsociacionesYActualizaClavesExistentes() {
        TablaHash<Integer, String> tabla = new TablaHash<>();
        for (int i = 0; i < 2_000; i++) {
            assertNull(tabla.poner(i, "v" + i));
        }

        assertEquals(2_000, tabla.size());
        for (int i = 0; i < 2_000; i++) {
            assertTrue(tabla.contieneClave(i));
            assertEquals("v" + i, tabla.obtener(i));
        }
        assertEquals("v500", tabla.poner(500, "actualizado"));
        assertEquals(2_000, tabla.size());
        assertEquals("actualizado", tabla.obtener(500));
    }

    @Test
    void clavesValoresYEliminacionDeUnaTablaVacia() {
        TablaHash<Integer, String> tabla = new TablaHash<>();
        assertEquals(0, tabla.size());
        assertFalse(tabla.contieneClave(1));
        assertNull(tabla.obtener(1));
        assertNull(tabla.eliminar(1));

        tabla.poner(1, "uno");
        tabla.poner(2, "dos");
        assertEquals(new HashSet<>(java.util.List.of(1, 2)), conjunto(tabla.claves()));
        assertEquals(new HashSet<>(java.util.List.of("uno", "dos")), conjunto(tabla.valores()));
        assertEquals("uno", tabla.eliminar(1));
        assertEquals(1, tabla.size());
    }

    private static <T> HashSet<T> conjunto(Iterable<T> elementos) {
        HashSet<T> resultado = new HashSet<>();
        elementos.forEach(resultado::add);
        return resultado;
    }

    private static final class ClaveColision {
        private final int id;

        private ClaveColision(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 7;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof ClaveColision clave && id == clave.id;
        }
    }
}
