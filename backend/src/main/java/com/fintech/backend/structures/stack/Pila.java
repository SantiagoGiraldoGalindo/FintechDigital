package com.fintech.backend.structures.stack;

import java.util.NoSuchElementException;

/**
 * Pila LIFO enlazada, apropiada para mantener operaciones recientes que se
 * pueden revertir en el proyecto.
 *
 * <p>push, pop, peek, size y estaVacia son O(1).</p>
 *
 * @param <T> tipo de elementos
 */
public class Pila<T> {

    private Nodo<T> cima;
    private int size;

    /** Agrega un elemento a la cima. Complejidad: O(1). */
    public void push(T elemento) {
        cima = new Nodo<>(elemento, cima);
        size++;
    }

    /**
     * Retira y devuelve el elemento de la cima.
     *
     * @throws NoSuchElementException si la pila está vacía
     */
    public T pop() {
        validarNoVacia();
        T elemento = cima.elemento;
        cima = cima.siguiente;
        size--;
        return elemento;
    }

    /**
     * Devuelve el elemento de la cima sin retirarlo.
     *
     * @throws NoSuchElementException si la pila está vacía
     */
    public T peek() {
        validarNoVacia();
        return cima.elemento;
    }

    /** Devuelve la cantidad de elementos. Complejidad: O(1). */
    public int size() {
        return size;
    }

    /** Indica si la pila está vacía. Complejidad: O(1). */
    public boolean estaVacia() {
        return size == 0;
    }

    private void validarNoVacia() {
        if (cima == null) {
            throw new NoSuchElementException("No se puede consultar o retirar de una pila vacía");
        }
    }

    private static final class Nodo<T> {
        private final T elemento;
        private final Nodo<T> siguiente;

        private Nodo(T elemento, Nodo<T> siguiente) {
            this.elemento = elemento;
            this.siguiente = siguiente;
        }
    }
}
