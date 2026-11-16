package com.fintech.backend.model;

import com.fintech.backend.enums.EstadoTransaccion;
import com.fintech.backend.enums.TipoOperacionProgramada;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** Operación pendiente de ejecutarse en una fecha programada. */
public class OperacionProgramada {

    private final Long idOperacion;
    private Transaccion transaccion;
    private LocalDateTime horaProgramada;
    private boolean transferenciasPeriodicas;
    private boolean recargasAutomaticas;
    private boolean pagosRecurrentes;
    private BigDecimal ahorroAutomaticoSemanal;
    private EstadoTransaccion estado;
    private TipoOperacionProgramada tipoOperacion;
    private BilleteraDigital billeteraOrigen;
    private BilleteraDigital billeteraDestino;
    private BigDecimal monto;
    private Duration periodicidad;
    private boolean notificacionProximaEnviada;

    public OperacionProgramada() {
        idOperacion = GeneradorIds.siguienteId();
        horaProgramada = LocalDateTime.now();
        ahorroAutomaticoSemanal = BigDecimal.ZERO;
        estado = EstadoTransaccion.PENDIENTE;
        periodicidad = Duration.ZERO;
    }

    public OperacionProgramada(Transaccion transaccion, LocalDateTime horaProgramada,
                               EstadoTransaccion estado) {
        this();
        setTransaccion(transaccion);
        setHoraProgramada(horaProgramada);
        setEstado(estado);
    }

    public Long getIdOperacion() { return idOperacion; }
    public Transaccion getTransaccion() { return transaccion; }
    public void setTransaccion(Transaccion transaccion) { this.transaccion = Objects.requireNonNull(transaccion, "La transacción no puede ser null"); }
    public LocalDateTime getHoraProgramada() { return horaProgramada; }
    public void setHoraProgramada(LocalDateTime horaProgramada) { this.horaProgramada = Objects.requireNonNull(horaProgramada, "La fecha programada no puede ser null"); }
    public boolean isTransferenciasPeriodicas() { return transferenciasPeriodicas; }
    public void setTransferenciasPeriodicas(boolean transferenciasPeriodicas) { this.transferenciasPeriodicas = transferenciasPeriodicas; }
    public boolean isRecargasAutomaticas() { return recargasAutomaticas; }
    public void setRecargasAutomaticas(boolean recargasAutomaticas) { this.recargasAutomaticas = recargasAutomaticas; }
    public boolean isPagosRecurrentes() { return pagosRecurrentes; }
    public void setPagosRecurrentes(boolean pagosRecurrentes) { this.pagosRecurrentes = pagosRecurrentes; }
    public BigDecimal getAhorroAutomaticoSemanal() { return ahorroAutomaticoSemanal; }
    public void setAhorroAutomaticoSemanal(BigDecimal ahorroAutomaticoSemanal) {
        Objects.requireNonNull(ahorroAutomaticoSemanal, "El ahorro semanal no puede ser null");
        if (ahorroAutomaticoSemanal.signum() < 0) throw new IllegalArgumentException("El ahorro semanal no puede ser negativo");
        this.ahorroAutomaticoSemanal = ahorroAutomaticoSemanal;
    }
    public EstadoTransaccion getEstado() { return estado; }
    public void setEstado(EstadoTransaccion estado) { this.estado = Objects.requireNonNull(estado, "El estado no puede ser null"); }
    public TipoOperacionProgramada getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(TipoOperacionProgramada tipoOperacion) {
        this.tipoOperacion = Objects.requireNonNull(tipoOperacion, "El tipo de operación no puede ser null");
    }
    public BilleteraDigital getBilleteraOrigen() { return billeteraOrigen; }
    public void setBilleteraOrigen(BilleteraDigital billeteraOrigen) { this.billeteraOrigen = billeteraOrigen; }
    public BilleteraDigital getBilleteraDestino() { return billeteraDestino; }
    public void setBilleteraDestino(BilleteraDigital billeteraDestino) { this.billeteraDestino = billeteraDestino; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) {
        this.monto = Objects.requireNonNull(monto, "El monto no puede ser null");
        if (monto.signum() <= 0) throw new IllegalArgumentException("El monto debe ser mayor que cero");
    }
    public Duration getPeriodicidad() { return periodicidad; }
    public void setPeriodicidad(Duration periodicidad) {
        this.periodicidad = Objects.requireNonNull(periodicidad, "La periodicidad no puede ser null");
        if (periodicidad.isZero() || periodicidad.isNegative()) {
            throw new IllegalArgumentException("La periodicidad debe ser positiva");
        }
    }
    public boolean isNotificacionProximaEnviada() { return notificacionProximaEnviada; }
    public void setNotificacionProximaEnviada(boolean notificacionProximaEnviada) {
        this.notificacionProximaEnviada = notificacionProximaEnviada;
    }
}
