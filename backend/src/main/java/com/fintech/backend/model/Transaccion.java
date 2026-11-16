package com.fintech.backend.model;

import com.fintech.backend.enums.EstadoTransaccion;
import com.fintech.backend.enums.NivelRiesgo;
import com.fintech.backend.enums.TipoTransaccion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Registro de una transacción y su estado actual. */
public class Transaccion {

    private final Long idTransaccion;
    private BigDecimal valor;
    private TipoTransaccion tipoTransaccion;
    private LocalDateTime fecha;
    private EstadoTransaccion estado;
    private BilleteraDigital billeteraOrigen;
    private BilleteraDigital billeteraDestino;
    private int puntosGenerados;
    private boolean duplicada;
    private NivelRiesgo nivelRiesgo;

    public Transaccion() {
        idTransaccion = GeneradorIds.siguienteId();
        fecha = LocalDateTime.now();
        estado = EstadoTransaccion.PENDIENTE;
    }

    public Transaccion(BigDecimal valor, TipoTransaccion tipoTransaccion,
                       BilleteraDigital billeteraOrigen, BilleteraDigital billeteraDestino) {
        this();
        setValor(valor);
        setTipoTransaccion(tipoTransaccion);
        this.billeteraOrigen = billeteraOrigen;
        this.billeteraDestino = billeteraDestino;
    }

    public Long getIdTransaccion() { return idTransaccion; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) {
        Objects.requireNonNull(valor, "El valor no puede ser null");
        if (valor.signum() < 0) throw new IllegalArgumentException("El valor no puede ser negativo");
        this.valor = valor;
    }
    public TipoTransaccion getTipoTransaccion() { return tipoTransaccion; }
    public void setTipoTransaccion(TipoTransaccion tipoTransaccion) { this.tipoTransaccion = Objects.requireNonNull(tipoTransaccion, "El tipo no puede ser null"); }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null"); }
    public EstadoTransaccion getEstado() { return estado; }
    public void setEstado(EstadoTransaccion estado) { this.estado = Objects.requireNonNull(estado, "El estado no puede ser null"); }
    public BilleteraDigital getBilleteraOrigen() { return billeteraOrigen; }
    public void setBilleteraOrigen(BilleteraDigital billeteraOrigen) { this.billeteraOrigen = billeteraOrigen; }
    public BilleteraDigital getBilleteraDestino() { return billeteraDestino; }
    public void setBilleteraDestino(BilleteraDigital billeteraDestino) { this.billeteraDestino = billeteraDestino; }
    public int getPuntosGenerados() { return puntosGenerados; }
    public void setPuntosGenerados(int puntosGenerados) {
        if (puntosGenerados < 0) throw new IllegalArgumentException("Los puntos generados no pueden ser negativos");
        this.puntosGenerados = puntosGenerados;
    }
    public boolean isDuplicada() { return duplicada; }
    public void setDuplicada(boolean duplicada) { this.duplicada = duplicada; }
    public NivelRiesgo getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(NivelRiesgo nivelRiesgo) {
        this.nivelRiesgo = Objects.requireNonNull(nivelRiesgo, "El nivel de riesgo no puede ser null");
    }
}
