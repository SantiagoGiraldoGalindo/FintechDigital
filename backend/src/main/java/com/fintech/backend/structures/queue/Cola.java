package com.fintech.backend.structures.queue;

import java.util.NoSuchElementException;

/**
 * Cola FIFO enlazada para elementos pendientes de procesamiento. Su variante
 * acotada conserva los elementos más recientes, adecuada para notificaciones.
 *
 * <p>Encolar, desencolar, frente, size y estaVacia son O(1).</p>
 *
 * @param <T> tipo de elementos
 */
public class Cola<T> {

    private Nodo<T> primero;
    private Nodo<T> ultimo;
    private final int capacidadMaxima;
    private int size;

    /** Crea una cola sin límite de capacidad. Complejidad: O(1). */
    public Cola() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Crea una cola acotada; al llenarse, encolar descarta el elemento más
     * antiguo antes de agregar el nuevo.
     *
     * @param capacidadMaxima capacidad positiva de la cola
     * @throws IllegalArgumentException si la capacidad no es positiva
     */
    public Cola(int capacidadMaxima) {
        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser positiva");
        }
        this.capacidadMaxima = capacidadMaxima;
    }

    /**
     * Agrega al final; si la cola acotada está llena, descarta el más antiguo.
     * Complejidad: O(1).
     */
    public void encolar(T elemento) {
        if (size == capacidadMaxima) {
            desencolar();
        }
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
     * Retira y devuelve el elemento más antiguo. Complejidad: O(1).
     *
     * @throws NoSuchElementException si la cola está vacía
     */
    public T desencolar() {
        validarNoVacia();
        T elemento = primero.elemento;
        primero = primero.siguiente;
        size--;
        if (primero == null) {
            ultimo = null;
        }
        return elemento;
    }

    /**
     * Devuelve el elemento más antiguo sin retirarlo. Complejidad: O(1).
     *
     * @throws NoSuchElementException si la cola está vacía
     */
    public T frente() {
        validarNoVacia();
        return primero.elemento;
    }

    /** Devuelve la cantidad de elementos. Complejidad: O(1). */
    public int size() {
        return size;
    }

    /** Indica si la cola está vacía. Complejidad: O(1). */
    public boolean estaVacia() {
        return size == 0;
    }

    private void validarNoVacia() {
        if (primero == null) {
            throw new NoSuchElementException("No se puede consultar o retirar de una cola vacía");
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
