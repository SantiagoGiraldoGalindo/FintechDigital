package com.fintech.backend.structures.hash;

import com.fintech.backend.structures.list.ListaEnlazada;
import java.util.Objects;

/**
 * Tabla hash con encadenamiento separado para búsquedas rápidas de asociaciones
 * (por ejemplo, usuarios por identificador). Usa una función de dispersión
 * propia y redimensiona al superar un factor de carga de 0.75.
 *
 * <p>poner, obtener, eliminar y contieneClave son O(1) en promedio y O(n) en el
 * peor caso. claves y valores son O(n).</p>
 *
 * @param <K> tipo de claves
 * @param <V> tipo de valores
 */
public class TablaHash<K, V> {

    private static final int CAPACIDAD_INICIAL = 16;
    private static final double FACTOR_CARGA_MAXIMO = 0.75;

    private Entrada<K, V>[] cubetas;
    private int size;

    /** Crea una tabla vacía. Complejidad: O(1). */
    @SuppressWarnings("unchecked")
    public TablaHash() {
        cubetas = (Entrada<K, V>[]) new Entrada[CAPACIDAD_INICIAL];
    }

    /**
     * Asocia una clave a un valor. Una clave existente reemplaza su valor sin
     * aumentar el tamaño; una nueva asociación puede provocar redimensionado.
     * Complejidad: O(1) en promedio.
     *
     * @return el valor anterior, o null si la clave no existía
     * @throws NullPointerException si la clave o el valor son null
     */
    public V poner(K clave, V valor) {
        Objects.requireNonNull(clave, "La clave no puede ser null");
        Objects.requireNonNull(valor, "El valor no puede ser null");
        int indice = indice(clave, cubetas.length);
        for (Entrada<K, V> entrada = cubetas[indice]; entrada != null; entrada = entrada.siguiente) {
            if (entrada.clave.equals(clave)) {
                V anterior = entrada.valor;
                entrada.valor = valor;
                return anterior;
            }
        }
        if (size + 1 > cubetas.length * FACTOR_CARGA_MAXIMO) {
            redimensionar();
            indice = indice(clave, cubetas.length);
        }
        cubetas[indice] = new Entrada<>(clave, valor, cubetas[indice]);
        size++;
        return null;
    }

    /**
     * Devuelve el valor asociado a la clave o null si no existe.
     * Complejidad: O(1) en promedio.
     *
     * @throws NullPointerException si la clave es null
     */
    public V obtener(K clave) {
        Objects.requireNonNull(clave, "La clave no puede ser null");
        Entrada<K, V> entrada = buscarEntrada(clave);
        return entrada == null ? null : entrada.valor;
    }

    /**
     * Elimina la asociación y devuelve su valor, o null si no existe.
     * Complejidad: O(1) en promedio.
     *
     * @throws NullPointerException si la clave es null
     */
    public V eliminar(K clave) {
        Objects.requireNonNull(clave, "La clave no puede ser null");
        int indice = indice(clave, cubetas.length);
        Entrada<K, V> anterior = null;
        Entrada<K, V> actual = cubetas[indice];
        while (actual != null) {
            if (actual.clave.equals(clave)) {
                if (anterior == null) {
                    cubetas[indice] = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                size--;
                return actual.valor;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return null;
    }

    /** Indica si la clave existe. Complejidad: O(1) en promedio. */
    public boolean contieneClave(K clave) {
        Objects.requireNonNull(clave, "La clave no puede ser null");
        return buscarEntrada(clave) != null;
    }

    /** Devuelve la cantidad de asociaciones. Complejidad: O(1). */
    public int size() {
        return size;
    }

    /** Devuelve las claves en orden de cubeta y encadenamiento. Complejidad: O(n). */
    public ListaEnlazada<K> claves() {
        ListaEnlazada<K> resultado = new ListaEnlazada<>();
        for (Entrada<K, V> cubeta : cubetas) {
            for (Entrada<K, V> entrada = cubeta; entrada != null; entrada = entrada.siguiente) {
                resultado.agregar(entrada.clave);
            }
        }
        return resultado;
    }

    /** Devuelve los valores en el mismo orden que {@link #claves()}. Complejidad: O(n). */
    public ListaEnlazada<V> valores() {
        ListaEnlazada<V> resultado = new ListaEnlazada<>();
        for (Entrada<K, V> cubeta : cubetas) {
            for (Entrada<K, V> entrada = cubeta; entrada != null; entrada = entrada.siguiente) {
                resultado.agregar(entrada.valor);
            }
        }
        return resultado;
    }

    private Entrada<K, V> buscarEntrada(K clave) {
        int indice = indice(clave, cubetas.length);
        for (Entrada<K, V> entrada = cubetas[indice]; entrada != null; entrada = entrada.siguiente) {
            if (entrada.clave.equals(clave)) {
                return entrada;
            }
        }
        return null;
    }

    private int indice(K clave, int capacidad) {
        int hash = clave.hashCode();
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        return (hash & 0x7fffffff) % capacidad;
    }

    @SuppressWarnings("unchecked")
    private void redimensionar() {
        Entrada<K, V>[] anterior = cubetas;
        cubetas = (Entrada<K, V>[]) new Entrada[anterior.length * 2];
        for (Entrada<K, V> cubeta : anterior) {
            Entrada<K, V> entrada = cubeta;
            while (entrada != null) {
                Entrada<K, V> siguiente = entrada.siguiente;
                int nuevoIndice = indice(entrada.clave, cubetas.length);
                entrada.siguiente = cubetas[nuevoIndice];
                cubetas[nuevoIndice] = entrada;
                entrada = siguiente;
            }
        }
    }

    private static final class Entrada<K, V> {
        private final K clave;
        private V valor;
        private Entrada<K, V> siguiente;

        private Entrada(K clave, V valor, Entrada<K, V> siguiente) {
            this.clave = clave;
            this.valor = valor;
            this.siguiente = siguiente;
        }
    }
}
