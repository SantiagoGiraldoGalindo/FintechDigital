package com.fintech.backend.service;

import com.fintech.backend.model.Transaccion;

/** Rechazo de una operación que ya quedó registrada como transacción fallida. */
public class OperacionRechazadaException extends RuntimeException {

    private final Transaccion transaccion;

    public OperacionRechazadaException(String message, Transaccion transaccion, Throwable cause) {
        super(message, cause);
        this.transaccion = transaccion;
    }

    public Transaccion getTransaccion() {
        return transaccion;
    }
}
