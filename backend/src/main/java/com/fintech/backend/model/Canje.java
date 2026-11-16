package com.fintech.backend.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Registro inmutable de un beneficio canjeado por un usuario. */
public class Canje {

    private final Long idCanje;
    private final Usuario usuario;
    private final Beneficio beneficio;
    private final int puntosDescontados;
    private final LocalDateTime fecha;

    public Canje(Usuario usuario, Beneficio beneficio, LocalDateTime fecha) {
        idCanje = GeneradorIds.siguienteId();
        this.usuario = Objects.requireNonNull(usuario, "El usuario no puede ser null");
        this.beneficio = Objects.requireNonNull(beneficio, "El beneficio no puede ser null");
        this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null");
        puntosDescontados = beneficio.getCostoPuntos();
    }

    public Long getIdCanje() { return idCanje; }
    public Usuario getUsuario() { return usuario; }
    public Beneficio getBeneficio() { return beneficio; }
    public int getPuntosDescontados() { return puntosDescontados; }
    public LocalDateTime getFecha() { return fecha; }
}
