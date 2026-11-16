package com.fintech.backend.model;

import com.fintech.backend.enums.TipoNotificacion;
import java.time.LocalDateTime;
import java.util.Objects;

/** Notificación asociada a un usuario. */
public class Notificacion {

    private final Long idNotificacion;
    private String mensaje;
    private LocalDateTime fecha;
    private TipoNotificacion tipoNotificacion;
    private Usuario usuario;

    public Notificacion() {
        idNotificacion = GeneradorIds.siguienteId();
        fecha = LocalDateTime.now();
    }

    public Notificacion(String mensaje, TipoNotificacion tipoNotificacion, Usuario usuario) {
        this();
        setMensaje(mensaje);
        setTipoNotificacion(tipoNotificacion);
        setUsuario(usuario);
    }

    public Long getIdNotificacion() { return idNotificacion; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = Objects.requireNonNull(mensaje, "El mensaje no puede ser null"); }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null"); }
    public TipoNotificacion getTipoNotificacion() { return tipoNotificacion; }
    public void setTipoNotificacion(TipoNotificacion tipoNotificacion) { this.tipoNotificacion = Objects.requireNonNull(tipoNotificacion, "El tipo de notificación no puede ser null"); }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = Objects.requireNonNull(usuario, "El usuario no puede ser null"); }
}
