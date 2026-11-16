package com.fintech.backend.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Evidencia inmutable de una regla de riesgo aplicada a una transacción. */
public class EventoAuditoria {

    private final LocalDateTime fecha;
    private final Transaccion transaccion;
    private final Alerta alerta;
    private final String regla;

    public EventoAuditoria(LocalDateTime fecha, Transaccion transaccion, Alerta alerta, String regla) {
        this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null");
        this.transaccion = Objects.requireNonNull(transaccion, "La transacción no puede ser null");
        this.alerta = Objects.requireNonNull(alerta, "La alerta no puede ser null");
        this.regla = Objects.requireNonNull(regla, "La regla no puede ser null");
    }

    public LocalDateTime getFecha() { return fecha; }
    public Transaccion getTransaccion() { return transaccion; }
    public Alerta getAlerta() { return alerta; }
    public String getRegla() { return regla; }
}
