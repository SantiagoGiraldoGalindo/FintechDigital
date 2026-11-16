package com.fintech.backend.service;

import com.fintech.backend.enums.TipoNotificacion;
import com.fintech.backend.model.Notificacion;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.structures.list.ListaEnlazada;
import com.fintech.backend.structures.queue.Cola;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * Registra notificaciones de usuario y administra su despacho FIFO global.
 * El saldo bajo se detecta al cruzar el umbral (10 por defecto), evitando
 * repetir el aviso en cada operación mientras el saldo ya permanece bajo.
 * Las operaciones programadas se consideran próximas durante las 24 horas
 * anteriores a su ejecución, intervalo configurable.
 */
public class NotificacionService {

    public static final int CAPACIDAD_HISTORIAL_RECIENTE = 20;
    private static final BigDecimal UMBRAL_SALDO_BAJO_PREDETERMINADO = new BigDecimal("10");
    private static final Duration VENTANA_PROXIMA_PREDETERMINADA = Duration.ofDays(1);

    private final Reloj reloj;
    private final Cola<Notificacion> pendientes = new Cola<>();
    private BigDecimal umbralSaldoBajo = UMBRAL_SALDO_BAJO_PREDETERMINADO;
    private Duration ventanaProxima = VENTANA_PROXIMA_PREDETERMINADA;

    public NotificacionService(Reloj reloj) {
        this.reloj = Objects.requireNonNull(reloj, "El reloj no puede ser null");
    }

    /**
     * Crea la notificación, la añade al historial completo y al historial
     * reciente acotado del usuario, y la encola para despacho.
     */
    public Notificacion notificar(Usuario usuario, TipoNotificacion tipo, String mensaje) {
        Objects.requireNonNull(usuario, "El usuario no puede ser null");
        Notificacion notificacion = new Notificacion(mensaje, tipo, usuario);
        notificacion.setFecha(reloj.ahora());
        usuario.getNotificaciones().agregar(notificacion);
        usuario.getNotificacionesRecientes().encolar(notificacion);
        pendientes.encolar(notificacion);
        return notificacion;
    }

    /** Retira y devuelve en FIFO todas las notificaciones pendientes. */
    public ListaEnlazada<Notificacion> despacharPendientes() {
        ListaEnlazada<Notificacion> despachadas = new ListaEnlazada<>();
        while (!pendientes.estaVacia()) {
            despachadas.agregar(pendientes.desencolar());
        }
        return despachadas;
    }

    public int cantidadPendientes() {
        return pendientes.size();
    }

    /** Devuelve copia FIFO del historial reciente sin consumirlo. */
    public ListaEnlazada<Notificacion> notificacionesRecientes(Usuario usuario) {
        Objects.requireNonNull(usuario, "El usuario no puede ser null");
        Cola<Notificacion> recientes = usuario.getNotificacionesRecientes();
        Cola<Notificacion> copiaCola = new Cola<>(CAPACIDAD_HISTORIAL_RECIENTE);
        ListaEnlazada<Notificacion> resultado = new ListaEnlazada<>();
        while (!recientes.estaVacia()) {
            Notificacion notificacion = recientes.desencolar();
            resultado.agregar(notificacion);
            copiaCola.encolar(notificacion);
        }
        while (!copiaCola.estaVacia()) recientes.encolar(copiaCola.desencolar());
        return resultado;
    }

    public BigDecimal getUmbralSaldoBajo() {
        return umbralSaldoBajo;
    }

    /** Configura el umbral inclusivo para notificaciones de saldo bajo. */
    public void setUmbralSaldoBajo(BigDecimal umbralSaldoBajo) {
        Objects.requireNonNull(umbralSaldoBajo, "El umbral no puede ser null");
        if (umbralSaldoBajo.signum() < 0) {
            throw new IllegalArgumentException("El umbral no puede ser negativo");
        }
        this.umbralSaldoBajo = umbralSaldoBajo;
    }

    public Duration getVentanaProxima() {
        return ventanaProxima;
    }

    /** Configura con cuánta anticipación se notifica una operación programada. */
    public void setVentanaProxima(Duration ventanaProxima) {
        Objects.requireNonNull(ventanaProxima, "La ventana no puede ser null");
        if (ventanaProxima.isNegative()) {
            throw new IllegalArgumentException("La ventana no puede ser negativa");
        }
        this.ventanaProxima = ventanaProxima;
    }
}
