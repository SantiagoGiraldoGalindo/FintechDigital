package com.fintech.backend.structures.queue;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Cola de prioridad implementada como montículo binario mínimo sobre un arreglo
 * dinámico propio. Sirve para procesar primero transacciones o alertas según
 * urgencia, definida por el comparador.
 *
 * <p>Insertar y extraer son O(log n); verPrimero y size son O(1). El arreglo
 * crece automáticamente y una ampliación cuesta O(n).</p>
 *
 * @param <T> tipo de elementos
 */
public class ColaPrioridad<T> {

    private static final int CAPACIDAD_INICIAL = 10;

    private final Comparator<? super T> comparador;
    private Object[] elementos = new Object[CAPACIDAD_INICIAL];
    private int size;

    /**
     * @param comparador determina el orden de prioridad; el menor elemento
     *                   según este comparador se extrae primero
     */
    public ColaPrioridad(Comparator<? super T> comparador) {
        this.comparador = Objects.requireNonNull(comparador, "El comparador no puede ser null");
    }

    /**
     * Inserta un elemento y restablece el orden del montículo. Complejidad:
     * O(log n), salvo ampliación del arreglo (O(n)).
     */
    public void insertar(T elemento) {
        Objects.requireNonNull(elemento, "El elemento no puede ser null");
        asegurarCapacidad();
        int indice = size++;
        elementos[indice] = elemento;
        siftUp(indice);
    }

    /**
     * Retira y devuelve el elemento de mayor prioridad (mínimo del comparador).
     * Complejidad: O(log n).
     *
     * @throws NoSuchElementException si la cola está vacía
     */
    public T extraer() {
        validarNoVacia();
        T primero = elementoEn(0);
        int ultimoIndice = --size;
        Object ultimo = elementos[ultimoIndice];
        elementos[ultimoIndice] = null;
        if (size > 0) {
            elementos[0] = ultimo;
            siftDown(0);
        }
        return primero;
    }

    /**
     * Devuelve el elemento de mayor prioridad sin retirarlo. Complejidad: O(1).
     *
     * @throws NoSuchElementException si la cola está vacía
     */
    public T verPrimero() {
        validarNoVacia();
        return elementoEn(0);
    }

    /** Devuelve la cantidad de elementos. Complejidad: O(1). */
    public int size() {
        return size;
    }

    private void siftUp(int indice) {
        while (indice > 0) {
            int padre = (indice - 1) / 2;
            if (comparar(elementoEn(indice), elementoEn(padre)) >= 0) {
                break;
            }
            intercambiar(indice, padre);
            indice = padre;
        }
    }

    private void siftDown(int indice) {
        while (true) {
            int izquierdo = indice * 2 + 1;
            if (izquierdo >= size) {
                return;
            }
            int derecho = izquierdo + 1;
            int menor = izquierdo;
            if (derecho < size
                    && comparar(elementoEn(derecho), elementoEn(izquierdo)) < 0) {
                menor = derecho;
            }
            if (comparar(elementoEn(indice), elementoEn(menor)) <= 0) {
                return;
            }
            intercambiar(indice, menor);
            indice = menor;
        }
    }

    private int comparar(T primero, T segundo) {
        return comparador.compare(primero, segundo);
    }

    private void intercambiar(int primero, int segundo) {
        Object temporal = elementos[primero];
        elementos[primero] = elementos[segundo];
        elementos[segundo] = temporal;
    }

    private void asegurarCapacidad() {
        if (size == elementos.length) {
            Object[] ampliado = new Object[elementos.length * 2];
            System.arraycopy(elementos, 0, ampliado, 0, size);
            elementos = ampliado;
        }
    }

    private T elementoEn(int indice) {
        @SuppressWarnings("unchecked")
        T elemento = (T) elementos[indice];
        return elemento;
    }

    private void validarNoVacia() {
        if (size == 0) {
            throw new NoSuchElementException(
                    "No se puede consultar o extraer de una cola de prioridad vacía");
        }
    }
}
