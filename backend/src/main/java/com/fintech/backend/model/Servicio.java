package com.fintech.backend.model;

import java.math.BigDecimal;
import java.util.Objects;
import com.fintech.backend.service.TransaccionService;

/** Fachada de operaciones financieras respaldada por {@link TransaccionService}. */
public class Servicio {

    private final Long idServicio;
    private Usuario usuario;
    private BilleteraDigital billetera;
    private Transaccion transaccion;
    private TransaccionService transaccionService;

    public Servicio() {
        idServicio = GeneradorIds.siguienteId();
    }

    public Servicio(TransaccionService transaccionService) {
        this();
        setTransaccionService(transaccionService);
    }

    public Servicio(Usuario usuario, BilleteraDigital billetera) {
        this();
        setUsuario(usuario);
        setBilletera(billetera);
    }

    public Transaccion realizarRecarga(BigDecimal valor) {
        transaccion = transacciones().realizarRecarga(billeteraActual(), valor);
        return transaccion;
    }

    public Transaccion realizarRetiro(BigDecimal valor) {
        transaccion = transacciones().realizarRetiro(billeteraActual(), valor);
        return transaccion;
    }

    public Transaccion realizarTransferencia(BilleteraDigital billeteraDestino, BigDecimal valor) {
        transaccion = transacciones().realizarTransaccion(billeteraActual(), billeteraDestino, valor);
        return transaccion;
    }

    public Transaccion realizarTransaccion(BilleteraDigital billeteraOrigen,
                                           BilleteraDigital billeteraDestino, BigDecimal valor) {
        transaccion = transacciones().realizarTransaccion(billeteraOrigen, billeteraDestino, valor);
        return transaccion;
    }

    public RevertirOperacion revertirOperacion(Transaccion transaccion, String motivo) {
        return transacciones().revertirOperacion(transaccion, motivo);
    }

    public BigDecimal consultarSaldo() {
        return transacciones().consultarSaldo(billeteraActual());
    }

    public BigDecimal consultarSaldo(BilleteraDigital billetera) {
        return transacciones().consultarSaldo(billetera);
    }

    public Long getIdServicio() { return idServicio; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = Objects.requireNonNull(usuario, "El usuario no puede ser null"); }
    public BilleteraDigital getBilletera() { return billetera; }
    public void setBilletera(BilleteraDigital billetera) { this.billetera = Objects.requireNonNull(billetera, "La billetera no puede ser null"); }
    public Transaccion getTransaccion() { return transaccion; }
    public void setTransaccion(Transaccion transaccion) { this.transaccion = transaccion; }
    public TransaccionService getTransaccionService() { return transaccionService; }
    public void setTransaccionService(TransaccionService transaccionService) {
        this.transaccionService = Objects.requireNonNull(transaccionService,
                "El servicio de transacciones no puede ser null");
    }

    private TransaccionService transacciones() {
        if (transaccionService == null) {
            throw new IllegalStateException("El servicio de transacciones no está configurado");
        }
        return transaccionService;
    }

    private BilleteraDigital billeteraActual() {
        return Objects.requireNonNull(billetera, "La billetera no está configurada");
    }
}
