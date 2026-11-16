
package com.fintech.backend.structures;

import com.fintech.backend.model.Transaccion;
import java.util.*;

public class GrafoTransaccion {

    private Map<Long, List<Transaccion>> conexiones;

    public GrafoTransaccion() {
        this.conexiones = new HashMap<>();
    }

    // Agregar una billetera al grafo
    public void agregarBilletera(Long idBilletera) {

        if (idBilletera == null) {
            throw new IllegalArgumentException(
                    "El ID de la billetera no puede ser nulo."
            );
        }

        conexiones.putIfAbsent(idBilletera, new ArrayList<>());
    }

    // Agregar una transferencia al grafo
    public void agregarTransaccion(Transaccion transaccion) {

        if (transaccion == null
                || transaccion.getBilleteraOrigen() == null
                || transaccion.getBilleteraDestino() == null) {
            throw new IllegalArgumentException(
                    "La transferencia debe tener billetera de origen y destino."
            );
        }

        Long origen = transaccion.getBilleteraOrigen().getIdBilletera();
        Long destino = transaccion.getBilleteraDestino().getIdBilletera();

        if (origen == null || destino == null) {
            throw new IllegalArgumentException(
                    "Las billeteras deben tener identificadores."
            );
        }

        agregarBilletera(origen);
        agregarBilletera(destino);

        conexiones.get(origen).add(transaccion);
    }

    // Consultar las transacciones de una billetera
    public List<Transaccion> obtenerTransacciones(Long idBilletera) {

        return new ArrayList<>(
                conexiones.getOrDefault(idBilletera, new ArrayList<>())
        );
    }

    // Consultar las billeteras conectadas
    public List<Long> obtenerDestinos(Long idBilletera) {

        List<Long> destinos = new ArrayList<>();

        for (Transaccion transaccion : obtenerTransacciones(idBilletera)) {

            Long destino =
                    transaccion.getBilleteraDestino().getIdBilletera();

            destinos.add(destino);
        }

        return destinos;
    }

    // Buscar si existe una conexión directa entre dos billeteras
    public boolean existeConexion(Long origen, Long destino) {

        for (Transaccion transaccion : obtenerTransacciones(origen)) {

            if (transaccion.getBilleteraDestino()
                    .getIdBilletera().equals(destino)) {
                return true;
            }
        }

        return false;
    }

    // Mostrar todas las billeteras registradas
    public Set<Long> obtenerBilleteras() {
        return new HashSet<>(conexiones.keySet());
    }

    // Consultar la cantidad de billeteras
    public int cantidadBilleteras() {
        return conexiones.size();
    }

    // Consultar la cantidad de conexiones
    public int cantidadTransacciones() {

        int total = 0;

        for (List<Transaccion> lista : conexiones.values()) {
            total += lista.size();
        }

        return total;
    }

    // Eliminar una transacción del grafo
    public boolean eliminarTransaccion(Long idTransaccion) {

        if (idTransaccion == null) {
            return false;
        }

        for (List<Transaccion> lista : conexiones.values()) {

            boolean eliminada = lista.removeIf(
                    transaccion -> transaccion.getIdTransaccion()
                            .equals(idTransaccion)
            );

            if (eliminada) {
                return true;
            }
        }

        return false;
    }

    // Eliminar una billetera del grafo
    public void eliminarBilletera(Long idBilletera) {

        conexiones.remove(idBilletera);

        for (List<Transaccion> lista : conexiones.values()) {

            lista.removeIf(transaccion ->
                    transaccion.getBilleteraDestino()
                            .getIdBilletera().equals(idBilletera)
            );
        }
    }
}
