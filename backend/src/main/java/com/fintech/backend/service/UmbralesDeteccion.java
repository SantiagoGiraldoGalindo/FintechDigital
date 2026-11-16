package com.fintech.backend.service;

import java.math.BigDecimal;
import java.time.Duration;

/** Umbrales de detección inusual; centralizados para permitir su ajuste operativo. */
public final class UmbralesDeteccion {

    public static final int TRANSFERENCIAS_SEGUIDAS = 3;
    public static final Duration VENTANA_TRANSFERENCIAS = Duration.ofMinutes(5);
    public static final BigDecimal MULTIPLICADOR_MONTO_PROMEDIO = BigDecimal.valueOf(3);
    public static final int REPETICIONES_MISMO_DESTINO = 3;
    public static final Duration VENTANA_REPETICION_DESTINO = Duration.ofMinutes(10);
    public static final int BILLETERAS_PARA_FRAGMENTACION = 3;
    public static final BigDecimal MONTO_MAXIMO_FRAGMENTADO = BigDecimal.valueOf(100);
    public static final Duration VENTANA_FRAGMENTACION = Duration.ofMinutes(10);
    public static final int OPERACIONES_FRECUENCIA_BRUSCA = 4;
    public static final Duration VENTANA_FRECUENCIA_BRUSCA = Duration.ofMinutes(5);
    public static final Duration INTERVALO_HABITUAL_MINIMO = Duration.ofMinutes(30);
    public static final int HISTORIAL_MINIMO_HORARIO = 3;
    public static final Duration TOLERANCIA_HORARIA = Duration.ofHours(1);

    private UmbralesDeteccion() {
    }
}
