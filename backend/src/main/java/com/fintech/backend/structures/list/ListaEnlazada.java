package com.fintech.backend.structures.list;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Lista enlazada simple para colecciones cuyo tamaño cambia durante la
 * ejecución. En el proyecto puede almacenar historiales de operaciones o
 * resultados sin requerir un arreglo de tamaño fijo.
 *
 * <p>Agregar al inicio y eliminar por índice son O(1) y O(n), respectivamente;
 * agregar al final, buscar, obtener y eliminar por índice son O(n). El tamaño
 * y la comprobación de vaciedad son O(1).</p>
 *
 * @param <T> tipo de elementos
 */
public class ListaEnlazada<T> implements Iterable<T> {

    private Nodo<T> primero;
    private Nodo<T> ultimo;
    private int size;

    /** Crea una lista vacía. Complejidad: O(1). */
    public ListaEnlazada() {
    }

    /**
     * Agrega un elemento al final, útil para mantener el orden de eventos.
     * Complejidad: O(1).
     */
    public void agregar(T elemento) {
        Nodo<T> nuevo = new Nodo<>(elemento);
        if (ultimo == null) {
            primero = nuevo;
        } else {
            ultimo.siguiente = nuevo;
        }
        ultimo = nuevo;
        size++;
    }

    /**
     * Agrega al inicio, por ejemplo para anteponer una operación reciente.
     * Complejidad: O(1).
     */
    public void agregarAlInicio(T elemento) {
        Nodo<T> nuevo = new Nodo<>(elemento);
        nuevo.siguiente = primero;
        primero = nuevo;
        if (ultimo == null) {
            ultimo = nuevo;
        }
        size++;
    }

    /**
     * Devuelve el elemento en el índice indicado.
     * Complejidad: O(n).
     *
     * @throws IndexOutOfBoundsException si el índice no está entre 0 y size-1
     */
    public T obtener(int indice) {
        return nodoEn(indice).elemento;
    }

    /**
     * Elimina y devuelve el elemento en el índice indicado.
     * Complejidad: O(n).
     *
     * @throws IndexOutOfBoundsException si el índice no está entre 0 y size-1
     */
    public T eliminar(int indice) {
        validarIndice(indice);
        Nodo<T> eliminado;
        if (indice == 0) {
            eliminado = primero;
            primero = primero.siguiente;
            if (primero == null) {
                ultimo = null;
            }
        } else {
            Nodo<T> anterior = nodoEn(indice - 1);
            eliminado = anterior.siguiente;
            anterior.siguiente = eliminado.siguiente;
            if (eliminado == ultimo) {
                ultimo = anterior;
            }
        }
        size--;
        return eliminado.elemento;
    }

    /**
     * Elimina la primera aparición del elemento.
     * Complejidad: O(n).
     *
     * @return true si se encontró y eliminó
     */
    public boolean eliminar(T elemento) {
        Nodo<T> anterior = null;
        Nodo<T> actual = primero;
        while (actual != null) {
            if (Objects.equals(actual.elemento, elemento)) {
                if (anterior == null) {
                    primero = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                if (actual == ultimo) {
                    ultimo = anterior;
                }
                size--;
                return true;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return false;
    }

    /** Indica si el elemento está presente. Complejidad: O(n). */
    public boolean contiene(T elemento) {
        for (T actual : this) {
            if (Objects.equals(actual, elemento)) {
                return true;
            }
        }
        return false;
    }

    /** Devuelve la cantidad de elementos. Complejidad: O(1). */
    public int size() {
        return size;
    }

    /** Indica si no hay elementos. Complejidad: O(1). */
    public boolean estaVacia() {
        return size == 0;
    }

    /** Elimina todos los elementos. Complejidad: O(1). */
    public void limpiar() {
        primero = null;
        ultimo = null;
        size = 0;
    }

    /**
     * Itera desde el primero hasta el último elemento. Crear el iterador cuesta
     * O(1); recorrer la lista completa cuesta O(n).
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = primero;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No hay más elementos en la lista");
                }
                T elemento = actual.elemento;
                actual = actual.siguiente;
                return elemento;
            }
        };
    }

    private Nodo<T> nodoEn(int indice) {
        validarIndice(indice);
        Nodo<T> actual = primero;
        for (int i = 0; i < indice; i++) {
            actual = actual.siguiente;
        }
        return actual;
    }

    private void validarIndice(int indice) {
        if (indice < 0 || indice >= size) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango para una lista de tamaño " + size);
        }
    }

    private static final class Nodo<T> {
        private final T elemento;
        private Nodo<T> siguiente;

        private Nodo(T elemento) {
            this.elemento = elemento;
        }
    }
}
