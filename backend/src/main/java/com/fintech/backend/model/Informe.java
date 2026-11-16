package com.fintech.backend.model;

import com.fintech.backend.structures.list.ListaEnlazada;
import com.fintech.backend.service.AnaliticaService;
import java.time.LocalDateTime;
import java.util.Objects;

/** Informe solicitado por un usuario sobre sus transacciones. */
public class Informe {

    private final Long idInforme;
    private String nombre;
    private LocalDateTime fecha;
    private String contenido;
    private String tipoInforme;
    private Usuario usuario;
    private ListaEnlazada<Transaccion> transacciones;

    public Informe() {
        idInforme = GeneradorIds.siguienteId();
        fecha = LocalDateTime.now();
        transacciones = new ListaEnlazada<>();
    }

    public Informe(String nombre, String tipoInforme, Usuario usuario) {
        this();
        setNombre(nombre);
        setTipoInforme(tipoInforme);
        this.usuario = usuario;
    }

    public String generarInforme(ListaEnlazada<Transaccion> transacciones) {
        Objects.requireNonNull(transacciones, "Las transacciones no pueden ser null");
        this.transacciones = new ListaEnlazada<>();
        for (Transaccion transaccion : transacciones) this.transacciones.agregar(transaccion);
        StringBuilder informe = new StringBuilder("Informe: ").append(nombre)
                .append("\nTipo: ").append(tipoInforme)
                .append("\nTransacciones: ").append(transacciones.size());
        this.contenido = informe.toString();
        return contenido;
    }

    /** Consolida las métricas y detecciones de una ventana temporal. */
    public String generarInforme(AnaliticaService analitica, LocalDateTime desde,
                                LocalDateTime hasta, int k) {
        Objects.requireNonNull(analitica, "El servicio de analítica no puede ser null");
        this.transacciones = analitica.transaccionesEnRango(desde, hasta);
        this.fecha = hasta;
        this.contenido = analitica.generarInforme(desde, hasta, k);
        return contenido;
    }

    public Long getIdInforme() { return idInforme; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = Objects.requireNonNull(nombre, "El nombre no puede ser null"); }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null"); }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = Objects.requireNonNull(contenido, "El contenido no puede ser null"); }
    public String getTipoInforme() { return tipoInforme; }
    public void setTipoInforme(String tipoInforme) { this.tipoInforme = Objects.requireNonNull(tipoInforme, "El tipo de informe no puede ser null"); }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = Objects.requireNonNull(usuario, "El usuario no puede ser null"); }
    public ListaEnlazada<Transaccion> getTransacciones() { return transacciones; }
    public void setTransacciones(ListaEnlazada<Transaccion> transacciones) { this.transacciones = Objects.requireNonNull(transacciones); }
}
