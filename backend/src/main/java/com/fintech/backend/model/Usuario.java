package com.fintech.backend.model;

import com.fintech.backend.enums.EstadoCuenta;
import com.fintech.backend.enums.NivelUsuario;
import com.fintech.backend.structures.list.ListaEnlazada;
import com.fintech.backend.structures.queue.Cola;
import com.fintech.backend.structures.stack.Pila;
import java.math.BigDecimal;
import java.util.Objects;

/** Usuario y colecciones de sus entidades relacionadas. */
public class Usuario {

    private final Long idUsuario;
    private String nombre;
    private String correo;
    private BigDecimal saldo;
    private NivelUsuario nivel;
    private int puntosAcumulados;
    private EstadoCuenta estado;
    private ListaEnlazada<BilleteraDigital> billeteras;
    private ListaEnlazada<Transaccion> transacciones;
    private Pila<Transaccion> operacionesReversibles;
    private ListaEnlazada<RevertirOperacion> historialReversiones;
    private ListaEnlazada<Notificacion> notificaciones;
    private Cola<Notificacion> notificacionesRecientes;
    private ListaEnlazada<Alerta> alertas;
    private ListaEnlazada<OperacionProgramada> operacionesProgramadas;
    private ListaEnlazada<Recompensas> recompensas;
    private ListaEnlazada<Informe> informes;

    public Usuario() {
        idUsuario = GeneradorIds.siguienteId();
        saldo = BigDecimal.ZERO;
        nivel = NivelUsuario.BRONCE;
        estado = EstadoCuenta.ACTIVO;
        billeteras = new ListaEnlazada<>();
        transacciones = new ListaEnlazada<>();
        operacionesReversibles = new Pila<>();
        historialReversiones = new ListaEnlazada<>();
        notificaciones = new ListaEnlazada<>();
        notificacionesRecientes = new Cola<>(20);
        alertas = new ListaEnlazada<>();
        operacionesProgramadas = new ListaEnlazada<>();
        recompensas = new ListaEnlazada<>();
        informes = new ListaEnlazada<>();
    }

    public Usuario(String nombre) {
        this();
        setNombre(nombre);
    }

    public Usuario(String nombre, String correo) {
        this(nombre);
        setCorreo(correo);
    }

    public Long getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        this.nombre = Objects.requireNonNull(nombre, "El nombre no puede ser null").trim();
        if (this.nombre.isEmpty()) throw new IllegalArgumentException("El nombre no puede estar vacío");
    }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) {
        this.correo = Objects.requireNonNull(correo, "El correo no puede ser null").trim();
        if (this.correo.isEmpty() || !this.correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El correo no tiene un formato válido");
        }
    }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) {
        Objects.requireNonNull(saldo, "El saldo no puede ser null");
        if (saldo.signum() < 0) throw new IllegalArgumentException("El saldo no puede ser negativo");
        this.saldo = saldo;
    }
    public NivelUsuario getNivel() { return nivel; }
    public void setNivel(NivelUsuario nivel) { this.nivel = Objects.requireNonNull(nivel, "El nivel no puede ser null"); }
    public int getPuntosAcumulados() { return puntosAcumulados; }
    public void setPuntosAcumulados(int puntosAcumulados) {
        if (puntosAcumulados < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos");
        this.puntosAcumulados = puntosAcumulados;
    }
    public EstadoCuenta getEstado() { return estado; }
    public void setEstado(EstadoCuenta estado) { this.estado = Objects.requireNonNull(estado, "El estado no puede ser null"); }
    public ListaEnlazada<BilleteraDigital> getBilleteras() { return billeteras; }
    public void setBilleteras(ListaEnlazada<BilleteraDigital> billeteras) { this.billeteras = Objects.requireNonNull(billeteras); }
    public ListaEnlazada<Transaccion> getTransacciones() { return transacciones; }
    public void setTransacciones(ListaEnlazada<Transaccion> transacciones) { this.transacciones = Objects.requireNonNull(transacciones); }
    public Pila<Transaccion> getOperacionesReversibles() { return operacionesReversibles; }
    public void setOperacionesReversibles(Pila<Transaccion> operacionesReversibles) {
        this.operacionesReversibles = Objects.requireNonNull(operacionesReversibles);
    }
    public ListaEnlazada<RevertirOperacion> getHistorialReversiones() { return historialReversiones; }
    public void setHistorialReversiones(ListaEnlazada<RevertirOperacion> historialReversiones) {
        this.historialReversiones = Objects.requireNonNull(historialReversiones);
    }
    public ListaEnlazada<Notificacion> getNotificaciones() { return notificaciones; }
    public void setNotificaciones(ListaEnlazada<Notificacion> notificaciones) { this.notificaciones = Objects.requireNonNull(notificaciones); }
    public Cola<Notificacion> getNotificacionesRecientes() { return notificacionesRecientes; }
    public void setNotificacionesRecientes(Cola<Notificacion> notificacionesRecientes) {
        this.notificacionesRecientes = Objects.requireNonNull(notificacionesRecientes);
    }
    public ListaEnlazada<Alerta> getAlertas() { return alertas; }
    public void setAlertas(ListaEnlazada<Alerta> alertas) { this.alertas = Objects.requireNonNull(alertas); }
    public ListaEnlazada<OperacionProgramada> getOperacionesProgramadas() { return operacionesProgramadas; }
    public void setOperacionesProgramadas(ListaEnlazada<OperacionProgramada> operacionesProgramadas) { this.operacionesProgramadas = Objects.requireNonNull(operacionesProgramadas); }
    public ListaEnlazada<Recompensas> getRecompensas() { return recompensas; }
    public void setRecompensas(ListaEnlazada<Recompensas> recompensas) { this.recompensas = Objects.requireNonNull(recompensas); }
    public ListaEnlazada<Informe> getInformes() { return informes; }
    public void setInformes(ListaEnlazada<Informe> informes) { this.informes = Objects.requireNonNull(informes); }
}
