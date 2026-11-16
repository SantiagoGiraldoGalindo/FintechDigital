package com.fintech.backend.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Solicitud para revertir una transacción. */
public class RevertirOperacion {

    private final Long idReversion;
    private Transaccion transaccion;
    private LocalDateTime fecha;
    private Usuario usuario;
    private BilleteraDigital billetera;
    private String motivo;

    public RevertirOperacion() {
        idReversion = GeneradorIds.siguienteId();
        fecha = LocalDateTime.now();
    }

    public RevertirOperacion(Transaccion transaccion, Usuario usuario,
                             BilleteraDigital billetera, String motivo) {
        this();
        setTransaccion(transaccion);
        setUsuario(usuario);
        this.billetera = billetera;
        setMotivo(motivo);
    }

    public void revertirOperacion() { throw new UnsupportedOperationException("pendiente paso 3"); }

    public Long getIdReversion() { return idReversion; }
    public Transaccion getTransaccion() { return transaccion; }
    public void setTransaccion(Transaccion transaccion) { this.transaccion = Objects.requireNonNull(transaccion, "La transacción no puede ser null"); }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null"); }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = Objects.requireNonNull(usuario, "El usuario no puede ser null"); }
    public BilleteraDigital getBilletera() { return billetera; }
    public void setBilletera(BilleteraDigital billetera) { this.billetera = billetera; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = Objects.requireNonNull(motivo, "El motivo no puede ser null"); }
}
