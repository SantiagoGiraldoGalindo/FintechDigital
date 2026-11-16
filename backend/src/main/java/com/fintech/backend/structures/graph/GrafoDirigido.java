package com.fintech.backend.structures.graph;

import com.fintech.backend.structures.hash.TablaHash;
import com.fintech.backend.structures.list.ListaEnlazada;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Grafo dirigido ponderado con adyacencia implementada mediante una tabla hash
 * y listas enlazadas. Permite representar rutas entre entidades y acumular el
 * monto y la frecuencia de cada conexión directa.
 *
 * <p>Agregar vértices/aristas, consultar vecinos y grados son O(1) en promedio,
 * salvo búsquedas dentro de la lista de vecinos. BFS, DFS, detección de ciclos
 * y recorridos completos son O(V + E). Los montos se acumulan con
 * {@link BigDecimal} para evitar errores de redondeo monetario.</p>
 *
 * @param <T> tipo de identificador de los vértices
 */
public class GrafoDirigido<T> {

    private final TablaHash<T, ListaEnlazada<Arista<T>>> adyacencia = new TablaHash<>();
    private int cantidadAristas;

    /** Agrega un vértice si no existe. Complejidad: O(1) en promedio. */
    public void agregarVertice(T vertice) {
        validarVertice(vertice);
        if (!adyacencia.contieneClave(vertice)) {
            adyacencia.poner(vertice, new ListaEnlazada<>());
        }
    }

    /**
     * Agrega o acumula una arista dirigida. Los extremos se agregan al grafo si
     * aún no existen. Llamadas repetidas entre el mismo par acumulan el monto y
     * cuentan como ocurrencias de una sola arista.
     *
     * @throws NullPointerException si algún extremo o el monto es null
     */
    public void agregarArista(T origen, T destino, BigDecimal monto) {
        validarVertice(origen);
        validarVertice(destino);
        Objects.requireNonNull(monto, "El monto no puede ser null");
        agregarVertice(origen);
        agregarVertice(destino);

        ListaEnlazada<Arista<T>> salientes = adyacencia.obtener(origen);
        for (Arista<T> arista : salientes) {
            if (arista.destino.equals(destino)) {
                arista.acumular(monto);
                return;
            }
        }
        salientes.agregar(new Arista<>(origen, destino, monto));
        cantidadAristas++;
    }

    /** Sobrecarga conveniente que convierte el monto a decimal. */
    public void agregarArista(T origen, T destino, double monto) {
        agregarArista(origen, destino, BigDecimal.valueOf(monto));
    }

    /**
     * Revierte una ocurrencia de arista, descontando su monto y frecuencia.
     * Elimina el par ordenado cuando ya no quedan ocurrencias.
     *
     * @throws NoSuchElementException si la arista no existe
     * @throws IllegalArgumentException si el monto es negativo
     */
    public void revertirArista(T origen, T destino, BigDecimal monto) {
        validarVertice(origen);
        validarVertice(destino);
        Objects.requireNonNull(monto, "El monto no puede ser null");
        if (monto.signum() < 0) throw new IllegalArgumentException("El monto no puede ser negativo");
        ListaEnlazada<Arista<T>> salientes = obtenerSalientes(origen);
        for (Arista<T> arista : salientes) {
            if (arista.destino.equals(destino)) {
                if (arista.veces == 1) {
                    if (arista.montoTotal.compareTo(monto) != 0) {
                        throw new IllegalStateException("El monto no coincide con la única ocurrencia de la arista");
                    }
                    salientes.eliminar(arista);
                } else {
                    arista.revertir(monto);
                }
                cantidadAristas--;
                return;
            }
        }
        throw new NoSuchElementException("No existe la arista dirigida " + origen + " -> " + destino);
    }

    /**
     * Devuelve los destinos directos del vértice en orden de inserción.
     * Complejidad: O(d), donde d es su grado de salida.
     *
     * @throws NoSuchElementException si el vértice no existe
     */
    public ListaEnlazada<T> vecinos(T vertice) {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        for (Arista<T> arista : obtenerSalientes(vertice)) {
            resultado.agregar(arista.destino);
        }
        return resultado;
    }

    /** Devuelve el número de aristas entrantes distintas. Complejidad: O(V+E). */
    public int gradoEntrada(T vertice) {
        validarExistencia(vertice);
        int grado = 0;
        for (T origen : adyacencia.claves()) {
            for (Arista<T> arista : adyacencia.obtener(origen)) {
                if (arista.destino.equals(vertice)) {
                    grado++;
                }
            }
        }
        return grado;
    }

    /** Devuelve el número de aristas salientes distintas. Complejidad: O(1). */
    public int gradoSalida(T vertice) {
        return obtenerSalientes(vertice).size();
    }

    /**
     * Recorre en anchura desde el vértice inicial; solo incluye vértices
     * alcanzables. Complejidad: O(V+E).
     */
    public ListaEnlazada<T> BFS(T inicio) {
        validarExistencia(inicio);
        ListaEnlazada<T> recorrido = new ListaEnlazada<>();
        TablaHash<T, Boolean> visitados = new TablaHash<>();
        ArrayDeque<T> pendientes = new ArrayDeque<>();
        visitados.poner(inicio, Boolean.TRUE);
        pendientes.addLast(inicio);
        while (!pendientes.isEmpty()) {
            T actual = pendientes.removeFirst();
            recorrido.agregar(actual);
            for (T vecino : vecinos(actual)) {
                if (!visitados.contieneClave(vecino)) {
                    visitados.poner(vecino, Boolean.TRUE);
                    pendientes.addLast(vecino);
                }
            }
        }
        return recorrido;
    }

    /**
     * Recorre en profundidad desde el vértice inicial; solo incluye vértices
     * alcanzables. Complejidad: O(V+E).
     */
    public ListaEnlazada<T> DFS(T inicio) {
        validarExistencia(inicio);
        ListaEnlazada<T> recorrido = new ListaEnlazada<>();
        TablaHash<T, Boolean> visitados = new TablaHash<>();
        recorrerDFS(inicio, visitados, recorrido);
        return recorrido;
    }

    /** Indica si existe una ruta dirigida entre ambos vértices. Complejidad: O(V+E). */
    public boolean existeCamino(T origen, T destino) {
        validarExistencia(origen);
        validarExistencia(destino);
        for (T vertice : BFS(origen)) {
            if (vertice.equals(destino)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detecta ciclos dirigidos mediante DFS con colores (blanco, gris y negro).
     * Cada arista hacia un vértice gris produce un ciclo representado por los
     * vértices del ciclo con el vértice inicial repetido al final. Se devuelve
     * un ciclo por cada arista de retroceso encontrada en el bosque DFS.
     * Complejidad: O(V+E), además del costo de materializar los ciclos.
     */
    public ListaEnlazada<ListaEnlazada<T>> detectarCiclos() {
        TablaHash<T, Integer> colores = new TablaHash<>();
        for (T vertice : adyacencia.claves()) {
            colores.poner(vertice, 0);
        }
        ListaEnlazada<ListaEnlazada<T>> ciclos = new ListaEnlazada<>();
        ListaEnlazada<T> camino = new ListaEnlazada<>();
        for (T vertice : adyacencia.claves()) {
            if (colores.obtener(vertice) == 0) {
                detectarCiclos(vertice, colores, camino, ciclos);
            }
        }
        return ciclos;
    }

    /**
     * Devuelve hasta k aristas ordenadas por ocurrencias descendentes. Los
     * empates conservan el orden de exploración del grafo. Complejidad:
     * O(E*k).
     *
     * @throws IllegalArgumentException si k es negativo
     */
    public ListaEnlazada<Arista<T>> aristasMasFrecuentes(int k) {
        if (k < 0) {
            throw new IllegalArgumentException("k no puede ser negativo");
        }
        ListaEnlazada<Arista<T>> pendientes = new ListaEnlazada<>();
        for (T origen : adyacencia.claves()) {
            for (Arista<T> arista : adyacencia.obtener(origen)) {
                pendientes.agregar(arista);
            }
        }
        ListaEnlazada<Arista<T>> resultado = new ListaEnlazada<>();
        while (!pendientes.estaVacia() && resultado.size() < k) {
            Arista<T> masFrecuente = pendientes.obtener(0);
            for (Arista<T> arista : pendientes) {
                if (arista.veces > masFrecuente.veces) {
                    masFrecuente = arista;
                }
            }
            resultado.agregar(masFrecuente);
            pendientes.eliminar(masFrecuente);
        }
        return resultado;
    }

    /**
     * Devuelve las aristas (rutas directas) que se han agregado al menos
     * minVeces, en orden de exploración. Complejidad: O(E).
     *
     * @throws IllegalArgumentException si minVeces es menor que uno
     */
    public ListaEnlazada<Arista<T>> rutasFrecuentes(int minVeces) {
        if (minVeces < 1) {
            throw new IllegalArgumentException("minVeces debe ser al menos 1");
        }
        ListaEnlazada<Arista<T>> resultado = new ListaEnlazada<>();
        for (T origen : adyacencia.claves()) {
            for (Arista<T> arista : adyacencia.obtener(origen)) {
                if (arista.veces >= minVeces) {
                    resultado.agregar(arista);
                }
            }
        }
        return resultado;
    }

    /** Devuelve el número de vértices registrados. Complejidad: O(1). */
    public int cantidadVertices() {
        return adyacencia.size();
    }

    /** Devuelve los vértices en orden de la tabla de adyacencia. */
    public ListaEnlazada<T> vertices() {
        return adyacencia.claves();
    }

    /** Devuelve la cantidad de pares ordenados con arista. Complejidad: O(1). */
    public int cantidadAristas() {
        return cantidadAristas;
    }

    private void recorrerDFS(T actual, TablaHash<T, Boolean> visitados,
                             ListaEnlazada<T> recorrido) {
        visitados.poner(actual, Boolean.TRUE);
        recorrido.agregar(actual);
        for (T vecino : vecinos(actual)) {
            if (!visitados.contieneClave(vecino)) {
                recorrerDFS(vecino, visitados, recorrido);
            }
        }
    }

    private void detectarCiclos(T actual, TablaHash<T, Integer> colores,
                                ListaEnlazada<T> camino,
                                ListaEnlazada<ListaEnlazada<T>> ciclos) {
        colores.poner(actual, 1);
        camino.agregar(actual);
        for (T vecino : vecinos(actual)) {
            int color = colores.obtener(vecino);
            if (color == 0) {
                detectarCiclos(vecino, colores, camino, ciclos);
            } else if (color == 1) {
                ciclos.agregar(cicloDesde(camino, vecino));
            }
        }
        camino.eliminar(camino.size() - 1);
        colores.poner(actual, 2);
    }

    private ListaEnlazada<T> cicloDesde(ListaEnlazada<T> camino, T inicio) {
        ListaEnlazada<T> ciclo = new ListaEnlazada<>();
        int indiceInicio = 0;
        while (!camino.obtener(indiceInicio).equals(inicio)) {
            indiceInicio++;
        }
        for (int i = indiceInicio; i < camino.size(); i++) {
            ciclo.agregar(camino.obtener(i));
        }
        ciclo.agregar(inicio);
        return ciclo;
    }

    private ListaEnlazada<Arista<T>> obtenerSalientes(T vertice) {
        validarExistencia(vertice);
        return adyacencia.obtener(vertice);
    }

    private void validarExistencia(T vertice) {
        validarVertice(vertice);
        if (!adyacencia.contieneClave(vertice)) {
            throw new NoSuchElementException("El vértice no existe en el grafo: " + vertice);
        }
    }

    private void validarVertice(T vertice) {
        Objects.requireNonNull(vertice, "El vértice no puede ser null");
    }

    /**
     * Arista dirigida agregada, con monto total y cantidad de ocurrencias.
     *
     * @param <T> tipo de identificador de vértice
     */
    public static final class Arista<T> {
        private final T origen;
        private final T destino;
        private BigDecimal montoTotal;
        private int veces;

        private Arista(T origen, T destino, BigDecimal monto) {
            this.origen = origen;
            this.destino = destino;
            this.montoTotal = monto;
            this.veces = 1;
        }

        public T getOrigen() {
            return origen;
        }

        public T getDestino() {
            return destino;
        }

        public BigDecimal getMontoTotal() {
            return montoTotal;
        }

        public int getVeces() {
            return veces;
        }

        private void acumular(BigDecimal monto) {
            montoTotal = montoTotal.add(monto);
            veces++;
        }

        private void revertir(BigDecimal monto) {
            if (montoTotal.compareTo(monto) < 0) {
                throw new IllegalStateException("El monto a revertir excede el total de la arista");
            }
            montoTotal = montoTotal.subtract(monto);
            veces--;
        }
    }
}
