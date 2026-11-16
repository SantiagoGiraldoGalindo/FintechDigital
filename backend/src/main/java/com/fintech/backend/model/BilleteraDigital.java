package com.fintech.backend.model;

import com.fintech.backend.enums.EstadoCuenta;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.structures.list.ListaEnlazada;
import java.math.BigDecimal;
import java.util.Objects;

/** Billetera digital asociada a un usuario. */
public class BilleteraDigital {

    private final Long idBilletera;
    private String codigo;
    private String nombre;
    private BigDecimal saldoDisponible;
    private EstadoCuenta estadoCuenta;
    private TipoBilletera tipoBilletera;
    private Usuario usuario;
    private ListaEnlazada<Transaccion> transacciones;

    public BilleteraDigital() {
        idBilletera = GeneradorIds.siguienteId();
        saldoDisponible = BigDecimal.ZERO;
        estadoCuenta = EstadoCuenta.ACTIVO;
        transacciones = new ListaEnlazada<>();
    }

    public BilleteraDigital(String nombre, TipoBilletera tipoBilletera, Usuario usuario) {
        this();
        setNombre(nombre);
        setTipoBilletera(tipoBilletera);
        setUsuario(usuario);
    }

    public BilleteraDigital(String codigo, String nombre, TipoBilletera tipoBilletera, Usuario usuario) {
        this(nombre, tipoBilletera, usuario);
        setCodigo(codigo);
    }

    public Long getIdBilletera() { return idBilletera; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) {
        this.codigo = Objects.requireNonNull(codigo, "El código no puede ser null").trim();
        if (this.codigo.isEmpty()) throw new IllegalArgumentException("El código no puede estar vacío");
    }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        this.nombre = Objects.requireNonNull(nombre, "El nombre no puede ser null").trim();
        if (this.nombre.isEmpty()) throw new IllegalArgumentException("El nombre no puede estar vacío");
    }
    public BigDecimal getSaldoDisponible() { return saldoDisponible; }
    public void setSaldoDisponible(BigDecimal saldoDisponible) {
        Objects.requireNonNull(saldoDisponible, "El saldo no puede ser null");
        if (saldoDisponible.signum() < 0) throw new IllegalArgumentException("El saldo no puede ser negativo");
        this.saldoDisponible = saldoDisponible;
    }
    public EstadoCuenta getEstadoCuenta() { return estadoCuenta; }
    public void setEstadoCuenta(EstadoCuenta estadoCuenta) { this.estadoCuenta = Objects.requireNonNull(estadoCuenta, "El estado no puede ser null"); }
    public TipoBilletera getTipoBilletera() { return tipoBilletera; }
    public void setTipoBilletera(TipoBilletera tipoBilletera) { this.tipoBilletera = Objects.requireNonNull(tipoBilletera, "El tipo de billetera no puede ser null"); }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = Objects.requireNonNull(usuario, "El usuario no puede ser null"); }
    public ListaEnlazada<Transaccion> getTransacciones() { return transacciones; }
    public void setTransacciones(ListaEnlazada<Transaccion> transacciones) {
        this.transacciones = Objects.requireNonNull(transacciones, "El historial no puede ser null");
    }
}
