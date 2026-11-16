package com.fintech.backend.service;

import com.fintech.backend.model.Transaccion;
import com.fintech.backend.structures.list.ListaEnlazada;
import java.time.Duration;
import java.util.Objects;

/** Avanza el reloj simulado y procesa las operaciones programadas vencidas. */
public class SimulacionService {

    private final Reloj reloj;
    private final OperacionProgramadaService operacionProgramadaService;

    public SimulacionService(Reloj reloj, OperacionProgramadaService operacionProgramadaService) {
        this.reloj = Objects.requireNonNull(reloj, "El reloj no puede ser null");
        this.operacionProgramadaService = Objects.requireNonNull(
                operacionProgramadaService, "El servicio de operaciones programadas no puede ser null");
    }

    public ListaEnlazada<Transaccion> avanzarYProcesar(Duration duracion) {
        reloj.avanzar(duracion);
        return operacionProgramadaService.procesarPendientes();
    }

    public java.time.LocalDateTime horaActual() {
        return reloj.ahora();
    }
}
