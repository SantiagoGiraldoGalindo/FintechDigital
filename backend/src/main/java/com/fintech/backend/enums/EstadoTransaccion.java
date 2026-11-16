package com.fintech.backend.enums;

/** Estados de una transacción (coinciden con la columna transacciones.estado). */
public enum EstadoTransaccion {
    PENDIENTE,
    COMPLETADA,
    FALLIDA,
    RECHAZADA,
    REVERTIDA
}
