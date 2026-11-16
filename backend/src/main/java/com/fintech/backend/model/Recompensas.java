package com.fintech.backend.model;

import com.fintech.backend.enums.NivelUsuario;
import com.fintech.backend.enums.TipoTransaccion;
import com.fintech.backend.structures.list.ListaEnlazada;
import java.math.BigDecimal;
import java.util.Objects;

/** Recompensas y beneficios disponibles o canjeados por un usuario. */
public class Recompensas {

    private final Long idRecompensa;
    private int costoPuntos;
    private ListaEnlazada<String> beneficiosDisponibles;
    private ListaEnlazada<String> beneficiosCanjeados;
    private ListaEnlazada<Canje> canjes;
    private Usuario usuario;

    public Recompensas() {
        idRecompensa = GeneradorIds.siguienteId();
        beneficiosDisponibles = new ListaEnlazada<>();
        beneficiosCanjeados = new ListaEnlazada<>();
        canjes = new ListaEnlazada<>();
    }

    public Recompensas(Usuario usuario) {
        this();
        setUsuario(usuario);
    }

    public int calcularPuntos(Transaccion transaccion) {
        Objects.requireNonNull(transaccion, "La transacción no puede ser null");
        BigDecimal valor = Objects.requireNonNull(transaccion.getValor(), "El valor no puede ser null");
        TipoTransaccion tipo = Objects.requireNonNull(
                transaccion.getTipoTransaccion(), "El tipo de transacción no puede ser null");
        int puntosPorCadaCien = switch (tipo) {
            case RECARGA -> 1;
            case RETIRO -> 2;
            case TRANSFERENCIA -> 3;
            case PAGO_PROGRAMADO -> 0;
        };
        return valor.divideToIntegralValue(BigDecimal.valueOf(100))
                .multiply(BigDecimal.valueOf(puntosPorCadaCien))
                .intValueExact();
    }

    public NivelUsuario calcularNivel(int puntosAcumulados) {
        if (puntosAcumulados < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos");
        if (puntosAcumulados <= 500) return NivelUsuario.BRONCE;
        if (puntosAcumulados <= 1_000) return NivelUsuario.PLATA;
        if (puntosAcumulados <= 5_000) return NivelUsuario.ORO;
        return NivelUsuario.PLATINO;
    }

    public Long getIdRecompensa() { return idRecompensa; }
    public int getCostoPuntos() { return costoPuntos; }
    public void setCostoPuntos(int costoPuntos) {
        if (costoPuntos < 0) throw new IllegalArgumentException("El costo en puntos no puede ser negativo");
        this.costoPuntos = costoPuntos;
    }
    public ListaEnlazada<String> getBeneficiosDisponibles() { return beneficiosDisponibles; }
    public void setBeneficiosDisponibles(ListaEnlazada<String> beneficiosDisponibles) { this.beneficiosDisponibles = Objects.requireNonNull(beneficiosDisponibles); }
    public ListaEnlazada<String> getBeneficiosCanjeados() { return beneficiosCanjeados; }
    public void setBeneficiosCanjeados(ListaEnlazada<String> beneficiosCanjeados) { this.beneficiosCanjeados = Objects.requireNonNull(beneficiosCanjeados); }
    public ListaEnlazada<Canje> getCanjes() { return canjes; }
    public void setCanjes(ListaEnlazada<Canje> canjes) { this.canjes = Objects.requireNonNull(canjes); }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = Objects.requireNonNull(usuario, "El usuario no puede ser null"); }
}
