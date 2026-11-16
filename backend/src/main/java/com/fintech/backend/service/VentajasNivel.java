package com.fintech.backend.service;

import com.fintech.backend.enums.NivelUsuario;

/**
 * Ventajas configurables por nivel: máximo de operaciones completadas por día
 * y bono fijo de puntos por operación completada.
 *
 * <p>Valores iniciales elegidos para el proyecto: Bronce 5 operaciones/0
 * puntos; Plata 10/5; Oro 20/10; Platino 50/20.</p>
 */
public final class VentajasNivel {

    public static final int BRONCE_LIMITE_DIARIO = 5;
    public static final int BRONCE_BONO_PUNTOS = 0;
    public static final int PLATA_LIMITE_DIARIO = 10;
    public static final int PLATA_BONO_PUNTOS = 5;
    public static final int ORO_LIMITE_DIARIO = 20;
    public static final int ORO_BONO_PUNTOS = 10;
    public static final int PLATINO_LIMITE_DIARIO = 50;
    public static final int PLATINO_BONO_PUNTOS = 20;

    private VentajasNivel() {
    }

    public static int limiteTransaccionesDiarias(NivelUsuario nivel) {
        return switch (nivel) {
            case BRONCE -> BRONCE_LIMITE_DIARIO;
            case PLATA -> PLATA_LIMITE_DIARIO;
            case ORO -> ORO_LIMITE_DIARIO;
            case PLATINO -> PLATINO_LIMITE_DIARIO;
        };
    }

    public static int bonoPuntos(NivelUsuario nivel) {
        return switch (nivel) {
            case BRONCE -> BRONCE_BONO_PUNTOS;
            case PLATA -> PLATA_BONO_PUNTOS;
            case ORO -> ORO_BONO_PUNTOS;
            case PLATINO -> PLATINO_BONO_PUNTOS;
        };
    }
}
