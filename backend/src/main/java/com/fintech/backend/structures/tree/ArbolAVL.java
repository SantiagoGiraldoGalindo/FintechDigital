package com.fintech.backend.structures.tree;

import com.fintech.backend.structures.list.ListaEnlazada;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Árbol AVL de búsqueda que asocia claves ordenables con valores. El balanceo
 * mantiene búsquedas, inserciones y eliminaciones eficientes; resulta útil para
 * consultar clasificaciones y rangos de puntuación en el proyecto.
 *
 * <p>Las claves son únicas según {@link Comparable#compareTo(Object)}. Insertar
 * una clave equivalente actualiza su valor y no cambia el tamaño. Para
 * clasificaciones que deban conservar empates, se debe componer la clave con
 * un identificador (por ejemplo, puntos + id), de modo que cada jugador tenga
 * una clave distinta.</p>
 *
 * <p>Insertar, eliminar y buscar son O(log n); mínimo y máximo son O(log n);
 * los recorridos y rangos son O(n) en el peor caso. Consultar tamaño es O(1).</p>
 *
 * @param <K> tipo de claves ordenables
 * @param <V> tipo de valores
 */
public class ArbolAVL<K extends Comparable<K>, V> {

    private Nodo<K, V> raiz;
    private int size;

    /**
     * Inserta una asociación o reemplaza el valor de una clave equivalente.
     * Complejidad: O(log n).
     *
     * @throws NullPointerException si la clave o el valor son null
     */
    public void insertar(K clave, V valor) {
        Objects.requireNonNull(clave, "La clave no puede ser null");
        Objects.requireNonNull(valor, "El valor no puede ser null");
        raiz = insertar(raiz, clave, valor);
    }

    /**
     * Busca una clave y devuelve su valor, o null si no existe.
     * Complejidad: O(log n).
     *
     * @throws NullPointerException si la clave es null
     */
    public V buscar(K clave) {
        Objects.requireNonNull(clave, "La clave no puede ser null");
        Nodo<K, V> actual = raiz;
        while (actual != null) {
            int comparacion = clave.compareTo(actual.clave);
            if (comparacion == 0) {
                return actual.valor;
            }
            actual = comparacion < 0 ? actual.izquierdo : actual.derecho;
        }
        return null;
    }

    /**
     * Elimina la clave y devuelve su valor, o null si no existía.
     * Complejidad: O(log n).
     *
     * @throws NullPointerException si la clave es null
     */
    public V eliminar(K clave) {
        Objects.requireNonNull(clave, "La clave no puede ser null");
        Resultado<V> resultado = new Resultado<>();
        raiz = eliminar(raiz, clave, resultado);
        if (resultado.encontrado) {
            size--;
        }
        return resultado.valor;
    }

    /**
     * Devuelve los valores cuyas claves están entre los límites, ambos
     * inclusivos, en orden ascendente de clave. Complejidad: O(log n + r), donde
     * r es la cantidad de resultados; O(n) en el peor caso.
     *
     * @throws NullPointerException si algún límite es null
     * @throws IllegalArgumentException si min es mayor que max
     */
    public ListaEnlazada<V> rango(K min, K max) {
        Objects.requireNonNull(min, "El límite mínimo no puede ser null");
        Objects.requireNonNull(max, "El límite máximo no puede ser null");
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("El límite mínimo no puede ser mayor que el máximo");
        }
        ListaEnlazada<V> resultado = new ListaEnlazada<>();
        agregarRango(raiz, min, max, resultado);
        return resultado;
    }

    /** Devuelve los valores en orden ascendente de clave. Complejidad: O(n). */
    public ListaEnlazada<V> recorridoInOrden() {
        ListaEnlazada<V> resultado = new ListaEnlazada<>();
        agregarInOrden(raiz, resultado);
        return resultado;
    }

    /** Devuelve los valores en orden descendente de clave. Complejidad: O(n). */
    public ListaEnlazada<V> recorridoDescendente() {
        ListaEnlazada<V> resultado = new ListaEnlazada<>();
        agregarDescendente(raiz, resultado);
        return resultado;
    }

    /**
     * Devuelve la menor clave. Complejidad: O(log n).
     *
     * @throws NoSuchElementException si el árbol está vacío
     */
    public K minimo() {
        return nodoMinimo(raiz).clave;
    }

    /**
     * Devuelve la mayor clave. Complejidad: O(log n).
     *
     * @throws NoSuchElementException si el árbol está vacío
     */
    public K maximo() {
        if (raiz == null) {
            throw new NoSuchElementException("No hay máximo: el árbol AVL está vacío");
        }
        Nodo<K, V> actual = raiz;
        while (actual.derecho != null) {
            actual = actual.derecho;
        }
        return actual.clave;
    }

    /** Devuelve la cantidad de claves distintas. Complejidad: O(1). */
    public int size() {
        return size;
    }

    private Nodo<K, V> insertar(Nodo<K, V> nodo, K clave, V valor) {
        if (nodo == null) {
            size++;
            return new Nodo<>(clave, valor);
        }
        int comparacion = clave.compareTo(nodo.clave);
        if (comparacion < 0) {
            nodo.izquierdo = insertar(nodo.izquierdo, clave, valor);
        } else if (comparacion > 0) {
            nodo.derecho = insertar(nodo.derecho, clave, valor);
        } else {
            nodo.valor = valor;
            return nodo;
        }
        return balancear(actualizarAltura(nodo));
    }

    private Nodo<K, V> eliminar(Nodo<K, V> nodo, K clave, Resultado<V> resultado) {
        if (nodo == null) {
            return null;
        }
        int comparacion = clave.compareTo(nodo.clave);
        if (comparacion < 0) {
            nodo.izquierdo = eliminar(nodo.izquierdo, clave, resultado);
        } else if (comparacion > 0) {
            nodo.derecho = eliminar(nodo.derecho, clave, resultado);
        } else {
            resultado.encontrado = true;
            resultado.valor = nodo.valor;
            if (nodo.izquierdo == null) {
                return nodo.derecho;
            }
            if (nodo.derecho == null) {
                return nodo.izquierdo;
            }
            Nodo<K, V> sucesor = nodoMinimo(nodo.derecho);
            nodo.clave = sucesor.clave;
            nodo.valor = sucesor.valor;
            nodo.derecho = eliminarSucesor(nodo.derecho);
        }
        return balancear(actualizarAltura(nodo));
    }

    private Nodo<K, V> eliminarSucesor(Nodo<K, V> nodo) {
        if (nodo.izquierdo == null) {
            return nodo.derecho;
        }
        nodo.izquierdo = eliminarSucesor(nodo.izquierdo);
        return balancear(actualizarAltura(nodo));
    }

    private void agregarRango(Nodo<K, V> nodo, K min, K max, ListaEnlazada<V> resultado) {
        if (nodo == null) {
            return;
        }
        int comparacionMin = min.compareTo(nodo.clave);
        int comparacionMax = max.compareTo(nodo.clave);
        if (comparacionMin < 0) {
            agregarRango(nodo.izquierdo, min, max, resultado);
        }
        if (comparacionMin <= 0 && comparacionMax >= 0) {
            resultado.agregar(nodo.valor);
        }
        if (comparacionMax > 0) {
            agregarRango(nodo.derecho, min, max, resultado);
        }
    }

    private void agregarInOrden(Nodo<K, V> nodo, ListaEnlazada<V> resultado) {
        if (nodo != null) {
            agregarInOrden(nodo.izquierdo, resultado);
            resultado.agregar(nodo.valor);
            agregarInOrden(nodo.derecho, resultado);
        }
    }

    private void agregarDescendente(Nodo<K, V> nodo, ListaEnlazada<V> resultado) {
        if (nodo != null) {
            agregarDescendente(nodo.derecho, resultado);
            resultado.agregar(nodo.valor);
            agregarDescendente(nodo.izquierdo, resultado);
        }
    }

    private Nodo<K, V> nodoMinimo(Nodo<K, V> nodo) {
        if (nodo == null) {
            throw new NoSuchElementException("No hay mínimo: el árbol AVL está vacío");
        }
        Nodo<K, V> actual = nodo;
        while (actual.izquierdo != null) {
            actual = actual.izquierdo;
        }
        return actual;
    }

    private Nodo<K, V> actualizarAltura(Nodo<K, V> nodo) {
        nodo.altura = 1 + Math.max(altura(nodo.izquierdo), altura(nodo.derecho));
        return nodo;
    }

    private int altura(Nodo<K, V> nodo) {
        return nodo == null ? 0 : nodo.altura;
    }

    private int factorBalance(Nodo<K, V> nodo) {
        return altura(nodo.izquierdo) - altura(nodo.derecho);
    }

    private Nodo<K, V> balancear(Nodo<K, V> nodo) {
        int balance = factorBalance(nodo);
        if (balance > 1) {
            if (factorBalance(nodo.izquierdo) < 0) {
                nodo.izquierdo = rotarIzquierda(nodo.izquierdo);
            }
            return rotarDerecha(nodo);
        }
        if (balance < -1) {
            if (factorBalance(nodo.derecho) > 0) {
                nodo.derecho = rotarDerecha(nodo.derecho);
            }
            return rotarIzquierda(nodo);
        }
        return nodo;
    }

    private Nodo<K, V> rotarDerecha(Nodo<K, V> raizSubarbol) {
        Nodo<K, V> nuevaRaiz = raizSubarbol.izquierdo;
        Nodo<K, V> subarbol = nuevaRaiz.derecho;
        nuevaRaiz.derecho = raizSubarbol;
        raizSubarbol.izquierdo = subarbol;
        actualizarAltura(raizSubarbol);
        return actualizarAltura(nuevaRaiz);
    }

    private Nodo<K, V> rotarIzquierda(Nodo<K, V> raizSubarbol) {
        Nodo<K, V> nuevaRaiz = raizSubarbol.derecho;
        Nodo<K, V> subarbol = nuevaRaiz.izquierdo;
        nuevaRaiz.izquierdo = raizSubarbol;
        raizSubarbol.derecho = subarbol;
        actualizarAltura(raizSubarbol);
        return actualizarAltura(nuevaRaiz);
    }

    private static final class Nodo<K, V> {
        private K clave;
        private V valor;
        private Nodo<K, V> izquierdo;
        private Nodo<K, V> derecho;
        private int altura = 1;

        private Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    private static final class Resultado<V> {
        private V valor;
        private boolean encontrado;
    }
}
