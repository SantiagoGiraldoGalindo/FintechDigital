package com.fintech.backend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Component;

/** Reloj inyectable que permite simular el paso del tiempo. */
@Component
public class Reloj {

    private LocalDateTime fechaHoraActual = LocalDateTime.now();

    public synchronized LocalDateTime ahora() {
        return fechaHoraActual;
    }

    public synchronized void avanzar(Duration duracion) {
        Objects.requireNonNull(duracion, "La duración no puede ser nula.");
        if (duracion.isNegative()) {
            throw new IllegalArgumentException("La duración no puede ser negativa.");
        }
        fechaHoraActual = fechaHoraActual.plus(duracion);
    }

    public synchronized void fijar(LocalDateTime fechaHora) {
        fechaHoraActual = Objects.requireNonNull(
                fechaHora,
                "La fecha y hora no pueden ser nulas.");
    }
}
