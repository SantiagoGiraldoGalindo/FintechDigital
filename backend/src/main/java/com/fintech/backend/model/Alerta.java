package com.fintech.backend.model;

import com.fintech.backend.enums.NivelRiesgo;
import java.time.LocalDateTime;
import java.util.Objects;

/** Alerta de riesgo relacionada con una operación o cuenta. */
public class Alerta {

    private final Long idAlerta;
    private Transaccion transaccion;
    private Usuario usuario;
    private BilleteraDigital billetera;
    private LocalDateTime fecha;
    private String mensaje;
    private NivelRiesgo nivelRiesgo;
    private String tipoAlerta;

    public Alerta() {
        idAlerta = GeneradorIds.siguienteId();
        fecha = LocalDateTime.now();
    }

    public Alerta(Transaccion transaccion, Usuario usuario, BilleteraDigital billetera,
                  String mensaje, NivelRiesgo nivelRiesgo, String tipoAlerta) {
        this();
        this.transaccion = transaccion;
        setUsuario(usuario);
        this.billetera = billetera;
        setMensaje(mensaje);
        setNivelRiesgo(nivelRiesgo);
        setTipoAlerta(tipoAlerta);
    }

    /** Registra esta alerta en el historial del usuario asociado. */
    public void generarAlerta() {
        Usuario titular = Objects.requireNonNull(usuario, "La alerta debe tener usuario");
        Objects.requireNonNull(mensaje, "La alerta debe tener mensaje");
        Objects.requireNonNull(nivelRiesgo, "La alerta debe tener nivel de riesgo");
        if (!titular.getAlertas().contiene(this)) titular.getAlertas().agregar(this);
    }

    /** Actualiza el detalle del evento y su fecha. */
    public void registrarEvento(String mensaje) {
        setMensaje(mensaje);
        setFecha(LocalDateTime.now());
    }

    /** Genera una descripción de auditoría de la alerta. */
    public String generarReporteEvento() {
        return "Alerta " + idAlerta + " [" + nivelRiesgo + "] " + tipoAlerta
                + " en " + fecha + ": " + mensaje;
    }

    public Long getIdAlerta() { return idAlerta; }
    public Transaccion getTransaccion() { return transaccion; }
    public void setTransaccion(Transaccion transaccion) { this.transaccion = transaccion; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = Objects.requireNonNull(usuario, "El usuario no puede ser null"); }
    public BilleteraDigital getBilletera() { return billetera; }
    public void setBilletera(BilleteraDigital billetera) { this.billetera = billetera; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null"); }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = Objects.requireNonNull(mensaje, "El mensaje no puede ser null"); }
    public NivelRiesgo getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(NivelRiesgo nivelRiesgo) { this.nivelRiesgo = Objects.requireNonNull(nivelRiesgo, "El nivel de riesgo no puede ser null"); }
    public String getTipoAlerta() { return tipoAlerta; }
    public void setTipoAlerta(String tipoAlerta) { this.tipoAlerta = Objects.requireNonNull(tipoAlerta, "El tipo de alerta no puede ser null"); }
}
