package com.fintech.backend.service;

import com.fintech.backend.enums.EstadoTransaccion;
import com.fintech.backend.enums.TipoNotificacion;
import com.fintech.backend.enums.TipoOperacionProgramada;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.OperacionProgramada;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.structures.list.ListaEnlazada;
import com.fintech.backend.structures.queue.ColaPrioridad;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

/**
 * Programa y procesa operaciones en orden de fecha, con prioridad de nivel
 * Platino a Bronce para fechas empatadas. Cada vencimiento recurrente se
 * reprograma sumando su periodicidad a la fecha anterior; si una ejecución falla
 * por saldo insuficiente, su transacción queda FALLIDA y el plan recurrente
 * sigue programado.
 */
public class OperacionProgramadaService {

    private final TransaccionService transaccionService;
    private final Reloj reloj;
    private final ColaPrioridad<EntradaProgramada> pendientes;
    private final ListaEnlazada<OperacionProgramada> operaciones = new ListaEnlazada<>();
    private final NotificacionService notificacionService;
    private long secuencia;

    public OperacionProgramadaService(TransaccionService transaccionService, Reloj reloj) {
        this.transaccionService = Objects.requireNonNull(
                transaccionService, "El servicio de transacciones no puede ser null");
        this.reloj = Objects.requireNonNull(reloj, "El reloj no puede ser null");
        notificacionService = transaccionService.getNotificacionService();
        pendientes = new ColaPrioridad<>(Comparator
                .comparing((EntradaProgramada entrada) -> entrada.fecha)
                .thenComparing(Comparator.comparingInt(
                        (EntradaProgramada entrada) -> entrada.prioridadNivel).reversed())
                .thenComparingLong(entrada -> entrada.secuencia));
    }

    public OperacionProgramada programarTransferenciaPeriodica(
            BilleteraDigital origen, BilleteraDigital destino, BigDecimal monto,
            LocalDateTime primeraEjecucion, Duration periodicidad) {
        return programar(TipoOperacionProgramada.TRANSFERENCIA_PERIODICA,
                origen, destino, monto, primeraEjecucion, periodicidad);
    }

    public OperacionProgramada programarRecargaAutomatica(
            BilleteraDigital destino, BigDecimal monto,
            LocalDateTime primeraEjecucion, Duration periodicidad) {
        return programar(TipoOperacionProgramada.RECARGA_AUTOMATICA,
                null, destino, monto, primeraEjecucion, periodicidad);
    }

    public OperacionProgramada programarPagoRecurrente(
            BilleteraDigital origen, BilleteraDigital destino, BigDecimal monto,
            LocalDateTime primeraEjecucion, Duration periodicidad) {
        return programar(TipoOperacionProgramada.PAGO_RECURRENTE,
                origen, destino, monto, primeraEjecucion, periodicidad);
    }

    /** Programa ahorro automático semanal entre dos billeteras del usuario. */
    public OperacionProgramada programarAhorroAutomaticoSemanal(
            BilleteraDigital origen, BilleteraDigital destino, BigDecimal monto,
            LocalDateTime primeraEjecucion) {
        return programar(TipoOperacionProgramada.AHORRO_AUTOMATICO_SEMANAL,
                origen, destino, monto, primeraEjecucion, Duration.ofDays(7));
    }

    /**
     * Programa una operación recurrente. Las periodicidades son positivas y se
     * representan con {@link Duration}; la fecha inicial puede estar en el
     * pasado y se procesará en la siguiente llamada a procesarPendientes.
     */
    public OperacionProgramada programar(TipoOperacionProgramada tipo,
                                         BilleteraDigital origen, BilleteraDigital destino,
                                         BigDecimal monto, LocalDateTime primeraEjecucion,
                                         Duration periodicidad) {
        Objects.requireNonNull(tipo, "El tipo de operación no puede ser null");
        Objects.requireNonNull(destino, "La billetera destino no puede ser null");
        Objects.requireNonNull(monto, "El monto no puede ser null");
        Objects.requireNonNull(primeraEjecucion, "La primera ejecución no puede ser null");
        if (monto.signum() <= 0) throw new IllegalArgumentException("El monto debe ser mayor que cero");
        Objects.requireNonNull(periodicidad, "La periodicidad no puede ser null");
        if (periodicidad.isZero() || periodicidad.isNegative()) {
            throw new IllegalArgumentException("La periodicidad debe ser positiva");
        }
        if (tipo != TipoOperacionProgramada.RECARGA_AUTOMATICA) {
            Objects.requireNonNull(origen, "La billetera origen no puede ser null");
            if (origen.getIdBilletera().equals(destino.getIdBilletera())) {
                throw new IllegalArgumentException("El origen y el destino deben ser distintos");
            }
            if (tipo == TipoOperacionProgramada.AHORRO_AUTOMATICO_SEMANAL
                    && !origen.getUsuario().getIdUsuario().equals(destino.getUsuario().getIdUsuario())) {
                throw new IllegalArgumentException("El ahorro automático debe usar billeteras del mismo usuario");
            }
        }

        OperacionProgramada operacion = new OperacionProgramada();
        operacion.setTipoOperacion(tipo);
        operacion.setBilleteraOrigen(origen);
        operacion.setBilleteraDestino(destino);
        operacion.setMonto(monto);
        operacion.setHoraProgramada(primeraEjecucion);
        operacion.setPeriodicidad(periodicidad);
        operacion.setEstado(EstadoTransaccion.PENDIENTE);
        switch (tipo) {
            case TRANSFERENCIA_PERIODICA -> operacion.setTransferenciasPeriodicas(true);
            case RECARGA_AUTOMATICA -> operacion.setRecargasAutomaticas(true);
            case PAGO_RECURRENTE -> operacion.setPagosRecurrentes(true);
            case AHORRO_AUTOMATICO_SEMANAL -> operacion.setAhorroAutomaticoSemanal(monto);
        }
        Usuario propietario = origen == null ? destino.getUsuario() : origen.getUsuario();
        Objects.requireNonNull(propietario, "La billetera destino debe tener usuario");
        propietario.getOperacionesProgramadas().agregar(operacion);
        operaciones.agregar(operacion);
        insertarEnCola(operacion);
        return operacion;
    }

    /**
     * Ejecuta todos los vencimientos hasta la hora actual del reloj y devuelve
     * las transacciones resultantes. Los planes recurrentes se reinsertan.
     */
    public ListaEnlazada<Transaccion> procesarPendientes() {
        ListaEnlazada<Transaccion> ejecutadas = new ListaEnlazada<>();
        notificarProximas();
        LocalDateTime ahora = reloj.ahora();
        while (pendientes.size() > 0 && !pendientes.verPrimero().fecha.isAfter(ahora)) {
            EntradaProgramada entrada = pendientes.extraer();
            OperacionProgramada operacion = entrada.operacion;
            try {
                Transaccion transaccion = ejecutar(operacion);
                operacion.setTransaccion(transaccion);
                operacion.setEstado(transaccion.getEstado());
                ejecutadas.agregar(transaccion);
            } catch (OperacionRechazadaException rechazo) {
                Transaccion transaccion = rechazo.getTransaccion();
                transaccion.setEstado(EstadoTransaccion.RECHAZADA);
                operacion.setTransaccion(transaccion);
                operacion.setEstado(EstadoTransaccion.RECHAZADA);
                ejecutadas.agregar(transaccion);
            }
            operacion.setHoraProgramada(entrada.fecha.plus(operacion.getPeriodicidad()));
            operacion.setNotificacionProximaEnviada(false);
            insertarEnCola(operacion);
        }
        return ejecutadas;
    }

    /**
     * Notifica una sola vez cuando la próxima ejecución entra en la ventana
     * configurable de anticipación. La ventana predeterminada es de 24 horas.
     */
    public void notificarProximas() {
        LocalDateTime ahora = reloj.ahora();
        LocalDateTime limite = ahora.plus(notificacionService.getVentanaProxima());
        for (OperacionProgramada operacion : operaciones) {
            LocalDateTime programada = operacion.getHoraProgramada();
            if (!operacion.isNotificacionProximaEnviada()
                    && !programada.isBefore(ahora) && !programada.isAfter(limite)) {
                BilleteraDigital titular = operacion.getBilleteraOrigen() == null
                        ? operacion.getBilleteraDestino() : operacion.getBilleteraOrigen();
                notificacionService.notificar(titular.getUsuario(),
                        TipoNotificacion.TRANSACCION_PROGRAMADA,
                        "La operación " + operacion.getIdOperacion()
                                + " está programada para " + programada);
                operacion.setNotificacionProximaEnviada(true);
            }
        }
    }

    public int cantidadPendientes() {
        return pendientes.size();
    }

    private Transaccion ejecutar(OperacionProgramada operacion) {
        return switch (operacion.getTipoOperacion()) {
            case TRANSFERENCIA_PERIODICA, AHORRO_AUTOMATICO_SEMANAL ->
                    transaccionService.realizarTransaccion(
                            operacion.getBilleteraOrigen(),
                            operacion.getBilleteraDestino(),
                            operacion.getMonto());
            case RECARGA_AUTOMATICA ->
                    transaccionService.realizarRecarga(
                            operacion.getBilleteraDestino(), operacion.getMonto());
            case PAGO_RECURRENTE ->
                    transaccionService.realizarPagoProgramado(
                            operacion.getBilleteraOrigen(),
                            operacion.getBilleteraDestino(),
                            operacion.getMonto());
        };
    }

    private void insertarEnCola(OperacionProgramada operacion) {
        pendientes.insertar(new EntradaProgramada(
                operacion, operacion.getHoraProgramada(),
                prioridad(operacion), secuencia++));
    }

    private static int prioridad(OperacionProgramada operacion) {
        BilleteraDigital billeteraPrioritaria = operacion.getBilleteraOrigen() == null
                ? operacion.getBilleteraDestino() : operacion.getBilleteraOrigen();
        Usuario usuario = billeteraPrioritaria.getUsuario();
        return usuario.getNivel().ordinal();
    }

    private static final class EntradaProgramada {
        private final OperacionProgramada operacion;
        private final LocalDateTime fecha;
        private final int prioridadNivel;
        private final long secuencia;

        private EntradaProgramada(OperacionProgramada operacion, LocalDateTime fecha,
                                  int prioridadNivel, long secuencia) {
            this.operacion = operacion;
            this.fecha = fecha;
            this.prioridadNivel = prioridadNivel;
            this.secuencia = secuencia;
        }
    }
}
