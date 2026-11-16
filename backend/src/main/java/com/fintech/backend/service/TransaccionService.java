package com.fintech.backend.service;

import com.fintech.backend.enums.EstadoCuenta;
import com.fintech.backend.enums.EstadoTransaccion;
import com.fintech.backend.enums.TipoNotificacion;
import com.fintech.backend.enums.TipoTransaccion;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.RevertirOperacion;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.structures.graph.GrafoDirigido;
import com.fintech.backend.structures.list.ListaEnlazada;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Ejecuta transacciones y mantiene historiales globales, por usuario y por
 * billetera. Las transferencias completadas se registran en grafos separados
 * de usuarios y billeteras. Recargas y transferencias se pueden deshacer;
 * un pago programado solo si se detectó duplicado. Se apilan por titular y
 * solo se deshacen en orden LIFO; retiros no son reversibles.
 */
public class TransaccionService {

    private static final Duration VENTANA_DUPLICIDAD_PREDETERMINADA = Duration.ofMinutes(5);
    private final UsuarioService usuarioService;
    private final BilleteraService billeteraService;
    private final Reloj reloj;
    private final RecompensasService recompensasService;
    private final NotificacionService notificacionService;
    private final AnaliticaService analiticaService;
    private final ListaEnlazada<Transaccion> transacciones = new ListaEnlazada<>();
    private final GrafoDirigido<Long> grafoUsuarios = new GrafoDirigido<>();
    private final GrafoDirigido<Long> grafoBilleteras = new GrafoDirigido<>();
    private Duration ventanaDuplicidad = VENTANA_DUPLICIDAD_PREDETERMINADA;

    public TransaccionService(UsuarioService usuarioService, BilleteraService billeteraService) {
        this(usuarioService, billeteraService, new Reloj());
    }

    public TransaccionService(UsuarioService usuarioService, BilleteraService billeteraService,
                              Reloj reloj) {
        this(usuarioService, billeteraService, reloj, new NotificacionService(reloj));
    }

    public TransaccionService(UsuarioService usuarioService, BilleteraService billeteraService,
                              Reloj reloj, RecompensasService recompensasService) {
        this(usuarioService, billeteraService, reloj, recompensasService,
                new NotificacionService(reloj));
    }

    public TransaccionService(UsuarioService usuarioService, BilleteraService billeteraService,
                              Reloj reloj, NotificacionService notificacionService) {
        this(usuarioService, billeteraService, reloj,
                new RecompensasService(usuarioService, notificacionService), notificacionService);
    }

    public TransaccionService(UsuarioService usuarioService, BilleteraService billeteraService,
                              Reloj reloj, RecompensasService recompensasService,
                              NotificacionService notificacionService) {
        this.usuarioService = Objects.requireNonNull(usuarioService, "El servicio de usuarios no puede ser null");
        this.billeteraService = Objects.requireNonNull(billeteraService, "El servicio de billeteras no puede ser null");
        this.reloj = Objects.requireNonNull(reloj, "El reloj no puede ser null");
        this.recompensasService = Objects.requireNonNull(recompensasService, "El servicio de recompensas no puede ser null");
        this.notificacionService = Objects.requireNonNull(notificacionService, "El servicio de notificaciones no puede ser null");
        this.recompensasService.setNotificacionService(notificacionService);
        this.analiticaService = new AnaliticaService(usuarioService, this, reloj);
    }

    public Transaccion realizarRecarga(BilleteraDigital destino, BigDecimal valor) {
        return ejecutar(TipoTransaccion.RECARGA, valor, null, destino);
    }

    public Transaccion realizarRetiro(BilleteraDigital origen, BigDecimal valor) {
        return ejecutar(TipoTransaccion.RETIRO, valor, origen, null);
    }

    public Transaccion realizarTransaccion(BilleteraDigital origen, BilleteraDigital destino,
                                           BigDecimal valor) {
        return ejecutar(TipoTransaccion.TRANSFERENCIA, valor, origen, destino);
    }

    /** Ejecuta un pago programado y aplica el bono del nivel si se completa. */
    public Transaccion realizarPagoProgramado(BilleteraDigital origen,
                                               BilleteraDigital destino, BigDecimal valor) {
        return ejecutar(TipoTransaccion.PAGO_PROGRAMADO, valor, origen, destino);
    }

    public Transaccion realizarRecarga(String codigoDestino, BigDecimal valor) {
        try {
            return realizarRecarga(resolverBilletera(codigoDestino), valor);
        } catch (OperacionRechazadaException exception) {
            throw exception;
        } catch (RuntimeException causa) {
            throw registrarRechazo(TipoTransaccion.RECARGA, valor, null, null, causa);
        }
    }

    public Transaccion realizarRetiro(String codigoOrigen, BigDecimal valor) {
        try {
            return realizarRetiro(resolverBilletera(codigoOrigen), valor);
        } catch (OperacionRechazadaException exception) {
            throw exception;
        } catch (RuntimeException causa) {
            throw registrarRechazo(TipoTransaccion.RETIRO, valor, null, null, causa);
        }
    }

    public Transaccion realizarTransaccion(String codigoOrigen, String codigoDestino,
                                           BigDecimal valor) {
        BilleteraDigital origen = null;
        try {
            origen = resolverBilletera(codigoOrigen);
            BilleteraDigital destino = resolverBilletera(codigoDestino);
            return realizarTransaccion(origen, destino, valor);
        } catch (OperacionRechazadaException exception) {
            throw exception;
        } catch (RuntimeException causa) {
            throw registrarRechazo(TipoTransaccion.TRANSFERENCIA, valor, origen, null, causa);
        }
    }

    public BigDecimal consultarSaldo(BilleteraDigital billetera) {
        validarBilleteraRegistrada(billetera);
        return billetera.getSaldoDisponible();
    }

    public BigDecimal consultarSaldo(String codigo) {
        return consultarSaldo(resolverBilletera(codigo));
    }

    /** Devuelve una copia enlazada del historial global. */
    public ListaEnlazada<Transaccion> historialGlobal() {
        return copiar(transacciones);
    }

    /** Devuelve el historial del usuario registrado con el id indicado. */
    public ListaEnlazada<Transaccion> historialPorUsuario(Long idUsuario) {
        return copiar(usuarioService.buscarUsuario(idUsuario).getTransacciones());
    }

    /** Devuelve el historial de la billetera registrada con el código indicado. */
    public ListaEnlazada<Transaccion> historialPorBilletera(String codigo) {
        return copiar(resolverBilletera(codigo).getTransacciones());
    }

    /**
     * Filtra el historial global en un rango de fechas inclusivo.
     *
     * @throws IllegalArgumentException si el inicio es posterior al fin
     */
    public ListaEnlazada<Transaccion> historialPorRango(LocalDateTime desde, LocalDateTime hasta) {
        Objects.requireNonNull(desde, "La fecha inicial no puede ser null");
        Objects.requireNonNull(hasta, "La fecha final no puede ser null");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        ListaEnlazada<Transaccion> resultado = new ListaEnlazada<>();
        for (Transaccion transaccion : transacciones) {
            if (!transaccion.getFecha().isBefore(desde) && !transaccion.getFecha().isAfter(hasta)) {
                resultado.agregar(transaccion);
            }
        }
        return resultado;
    }

    public GrafoDirigido<Long> getGrafoUsuarios() {
        return grafoUsuarios;
    }

    public GrafoDirigido<Long> getGrafoBilleteras() {
        return grafoBilleteras;
    }

    public RecompensasService getRecompensasService() {
        return recompensasService;
    }

    public NotificacionService getNotificacionService() {
        return notificacionService;
    }

    public AnaliticaService getAnaliticaService() {
        return analiticaService;
    }

    public int cantidadTransacciones() {
        return transacciones.size();
    }

    /**
     * Revierte la transacción completada que está en la cima de la pila
     * reversible del usuario titular. Si el destinatario ya gastó el monto o
     * el titular gastó los puntos generados, no modifica nada y conserva la
     * transacción en la pila para permitir reintentar tras corregir la causa.
     */
    public RevertirOperacion revertirOperacion(Transaccion transaccion, String motivo) {
        Objects.requireNonNull(transaccion, "La transacción no puede ser null");
        Objects.requireNonNull(motivo, "El motivo no puede ser null");
        if (motivo.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
        if (!transacciones.contiene(transaccion)) {
            throw new NoSuchElementException("La transacción no pertenece a este servicio");
        }
        if (transaccion.getEstado() == EstadoTransaccion.REVERTIDA) {
            throw new IllegalStateException("La operación ya fue revertida");
        }
        if (transaccion.getEstado() != EstadoTransaccion.COMPLETADA) {
            throw new IllegalStateException("Solo se pueden revertir operaciones completadas");
        }
        validarTipoReversible(transaccion);

        BilleteraDigital origen = transaccion.getBilleteraOrigen();
        BilleteraDigital destino = transaccion.getBilleteraDestino();
        Usuario titular = transaccion.getTipoTransaccion() == TipoTransaccion.RECARGA
                ? destino.getUsuario() : origen.getUsuario();
        if (titular.getOperacionesReversibles().estaVacia()
                || titular.getOperacionesReversibles().peek() != transaccion) {
            throw new IllegalStateException(
                    "Solo se puede revertir la operación reversible más reciente del usuario");
        }
        if (destino.getSaldoDisponible().compareTo(transaccion.getValor()) < 0) {
            throw new IllegalStateException(
                    "No se puede revertir: el destino ya no tiene saldo suficiente; no se modifica la operación");
        }
        if (titular.getPuntosAcumulados() < transaccion.getPuntosGenerados()) {
            throw new IllegalStateException(
                    "No se puede revertir: el usuario ya no tiene los puntos generados disponibles");
        }

        if (transaccion.getTipoTransaccion() == TipoTransaccion.TRANSFERENCIA
                || transaccion.getTipoTransaccion() == TipoTransaccion.PAGO_PROGRAMADO) {
            grafoUsuarios.revertirArista(
                    origen.getUsuario().getIdUsuario(), destino.getUsuario().getIdUsuario(),
                    transaccion.getValor());
            grafoBilleteras.revertirArista(
                    origen.getIdBilletera(), destino.getIdBilletera(), transaccion.getValor());
            origen.setSaldoDisponible(origen.getSaldoDisponible().add(transaccion.getValor()));
        }
        destino.setSaldoDisponible(destino.getSaldoDisponible().subtract(transaccion.getValor()));
        actualizarSaldoUsuarios(origen, destino);
        usuarioService.ajustarPuntos(titular.getIdUsuario(), -transaccion.getPuntosGenerados());
        titular.getOperacionesReversibles().pop();
        transaccion.setEstado(EstadoTransaccion.REVERTIDA);

        RevertirOperacion reversion = new RevertirOperacion(transaccion, titular, destino, motivo);
        reversion.setFecha(reloj.ahora());
        titular.getHistorialReversiones().agregar(reversion);
        return reversion;
    }

    /** Devuelve una copia del historial de deshacer de un usuario. */
    public ListaEnlazada<RevertirOperacion> historialReversiones(Long idUsuario) {
        ListaEnlazada<RevertirOperacion> historial = new ListaEnlazada<>();
        for (RevertirOperacion reversion
                : usuarioService.buscarUsuario(idUsuario).getHistorialReversiones()) {
            historial.agregar(reversion);
        }
        return historial;
    }

    /** Ventana inclusiva para marcar transacciones repetidas; predeterminada: cinco minutos. */
    public Duration getVentanaDuplicidad() {
        return ventanaDuplicidad;
    }

    public void setVentanaDuplicidad(Duration ventanaDuplicidad) {
        Objects.requireNonNull(ventanaDuplicidad, "La ventana de duplicidad no puede ser null");
        if (ventanaDuplicidad.isNegative()) {
            throw new IllegalArgumentException("La ventana de duplicidad no puede ser negativa");
        }
        this.ventanaDuplicidad = ventanaDuplicidad;
    }

    private Transaccion ejecutar(TipoTransaccion tipo, BigDecimal valor,
                                 BilleteraDigital origen, BilleteraDigital destino) {
        Transaccion transaccion = nuevaTransaccion(tipo, valor, origen, destino);
        Usuario usuarioOperacion = null;
        int puntosOperacion = 0;
        try {
            validarMonto(valor);
            if (origen != null) validarBilleteraActiva(origen);
            if (destino != null) validarBilleteraActiva(destino);

            if (tipo == TipoTransaccion.TRANSFERENCIA || tipo == TipoTransaccion.PAGO_PROGRAMADO) {
                if (origen == destino || origen.getIdBilletera().equals(destino.getIdBilletera())) {
                    throw new IllegalArgumentException("El origen y el destino deben ser billeteras distintas");
                }
                validarUsuarioExistente(origen);
                validarUsuarioExistente(destino);
                if (origen.getSaldoDisponible().compareTo(valor) < 0) {
                    throw new IllegalStateException("Saldo insuficiente en la billetera de origen");
                }
            } else if (tipo == TipoTransaccion.RETIRO) {
                validarUsuarioExistente(origen);
                if (origen.getSaldoDisponible().compareTo(valor) < 0) {
                    throw new IllegalStateException("Saldo insuficiente en la billetera");
                }
            } else {
                validarUsuarioExistente(destino);
            }
            usuarioOperacion = tipo == TipoTransaccion.RECARGA
                    ? destino.getUsuario() : origen.getUsuario();
            recompensasService.validarOperacion(usuarioOperacion, transaccion.getFecha());
            puntosOperacion = recompensasService.calcularPuntosOperacion(usuarioOperacion, transaccion);
        } catch (RuntimeException causa) {
            throw registrarRechazo(transaccion, causa);
        }
        BigDecimal saldoOrigenAnterior = origen == null ? null : origen.getSaldoDisponible();
        if (origen != null) origen.setSaldoDisponible(origen.getSaldoDisponible().subtract(valor));
        if (destino != null) destino.setSaldoDisponible(destino.getSaldoDisponible().add(valor));
        actualizarSaldoUsuarios(origen, destino);
        transaccion.setEstado(EstadoTransaccion.COMPLETADA);
        recompensasService.aplicarOperacion(usuarioOperacion, transaccion, puntosOperacion);
        transaccion.setDuplicada(esDuplicada(transaccion));
        registrar(transaccion);
        analiticaService.detectarAnomalias(transaccion);
        if (tipo == TipoTransaccion.TRANSFERENCIA || tipo == TipoTransaccion.PAGO_PROGRAMADO) {
            grafoUsuarios.agregarArista(origen.getUsuario().getIdUsuario(),
                    destino.getUsuario().getIdUsuario(), valor);
            grafoBilleteras.agregarArista(origen.getIdBilletera(),
                    destino.getIdBilletera(), valor);
        }
        if (esTipoReversible(transaccion)) {
            usuarioOperacion.getOperacionesReversibles().push(transaccion);
        }
        if (origen != null && saldoOrigenAnterior.compareTo(notificacionService.getUmbralSaldoBajo()) > 0
                && origen.getSaldoDisponible().compareTo(notificacionService.getUmbralSaldoBajo()) <= 0) {
            notificacionService.notificar(origen.getUsuario(), TipoNotificacion.SALDO_BAJO,
                    "El saldo de la billetera " + origen.getCodigo() + " es "
                            + origen.getSaldoDisponible() + ", igual o inferior al umbral configurado");
        }
        return transaccion;
    }

    private boolean esDuplicada(Transaccion candidata) {
        if (!puedeDetectarseDuplicada(candidata)) return false;
        LocalDateTime limiteInferior = candidata.getFecha().minus(ventanaDuplicidad);
        for (Transaccion anterior : transacciones) {
            if (anterior.getEstado() == EstadoTransaccion.COMPLETADA
                    && puedeDetectarseDuplicada(anterior)
                    && mismaBilletera(anterior.getBilleteraOrigen(), candidata.getBilleteraOrigen())
                    && mismaBilletera(anterior.getBilleteraDestino(), candidata.getBilleteraDestino())
                    && anterior.getValor().compareTo(candidata.getValor()) == 0
                    && !anterior.getFecha().isBefore(limiteInferior)
                    && !anterior.getFecha().isAfter(candidata.getFecha())) {
                return true;
            }
        }
        return false;
    }

    private static boolean mismaBilletera(BilleteraDigital primero, BilleteraDigital segundo) {
        if (primero == null || segundo == null) return primero == segundo;
        return primero.getIdBilletera().equals(segundo.getIdBilletera());
    }

    private static boolean esTipoReversible(Transaccion transaccion) {
        return transaccion.getTipoTransaccion() == TipoTransaccion.RECARGA
                || transaccion.getTipoTransaccion() == TipoTransaccion.TRANSFERENCIA
                || (transaccion.getTipoTransaccion() == TipoTransaccion.PAGO_PROGRAMADO
                    && transaccion.isDuplicada());
    }

    private static boolean puedeDetectarseDuplicada(Transaccion transaccion) {
        return transaccion.getTipoTransaccion() != TipoTransaccion.RETIRO;
    }

    private static void validarTipoReversible(Transaccion transaccion) {
        if (!esTipoReversible(transaccion)) {
            throw new IllegalArgumentException(
                    "Solo son reversibles recargas, transferencias y operaciones duplicadas; "
                            + "los retiros no se pueden revertir");
        }
    }

    private OperacionRechazadaException registrarRechazo(TipoTransaccion tipo, BigDecimal valor,
                                                          BilleteraDigital origen,
                                                          BilleteraDigital destino,
                                                          RuntimeException causa) {
        return registrarRechazo(nuevaTransaccion(tipo, valor, origen, destino), causa);
    }

    private OperacionRechazadaException registrarRechazo(Transaccion transaccion,
                                                          RuntimeException causa) {
        transaccion.setEstado(EstadoTransaccion.FALLIDA);
        registrar(transaccion);
        notificarRechazo(transaccion, causa);
        return new OperacionRechazadaException(mensajeDominio(causa), transaccion, causa);
    }

    private void notificarRechazo(Transaccion transaccion, RuntimeException causa) {
        Usuario origen = usuarioRegistrado(transaccion.getBilleteraOrigen());
        Usuario destino = usuarioRegistrado(transaccion.getBilleteraDestino());
        String mensaje = "La operación " + transaccion.getTipoTransaccion()
                + " fue rechazada: " + mensajeDominio(causa);
        if (origen != null) {
            notificacionService.notificar(origen, TipoNotificacion.OPERACION_RECHAZADA, mensaje);
        }
        if (destino != null && (origen == null
                || !destino.getIdUsuario().equals(origen.getIdUsuario()))) {
            notificacionService.notificar(destino, TipoNotificacion.OPERACION_RECHAZADA, mensaje);
        }
    }

    private Usuario usuarioRegistrado(BilleteraDigital billetera) {
        if (billetera == null || billetera.getUsuario() == null) return null;
        try {
            Usuario usuario = usuarioService.buscarUsuario(billetera.getUsuario().getIdUsuario());
            return usuario == billetera.getUsuario() ? usuario : null;
        } catch (NoSuchElementException exception) {
            return null;
        }
    }

    private Transaccion nuevaTransaccion(TipoTransaccion tipo, BigDecimal valor,
                                        BilleteraDigital origen, BilleteraDigital destino) {
        BigDecimal valorRegistrado = valor == null || valor.signum() < 0 ? BigDecimal.ZERO : valor;
        Transaccion transaccion = new Transaccion(valorRegistrado, tipo, origen, destino);
        transaccion.setFecha(reloj.ahora());
        transaccion.setPuntosGenerados(0);
        return transaccion;
    }

    private void registrar(Transaccion transaccion) {
        transacciones.agregar(transaccion);
        registrarEnBilletera(transaccion.getBilleteraOrigen(), transaccion);
        registrarEnBilletera(transaccion.getBilleteraDestino(), transaccion);
        registrarEnUsuario(transaccion.getBilleteraOrigen(), transaccion);
        if (transaccion.getBilleteraDestino() == null
                || transaccion.getBilleteraOrigen() == null
                || !mismoUsuario(transaccion.getBilleteraOrigen(), transaccion.getBilleteraDestino())) {
            registrarEnUsuario(transaccion.getBilleteraDestino(), transaccion);
        }
    }

    private void registrarEnBilletera(BilleteraDigital billetera, Transaccion transaccion) {
        if (billetera != null && !billetera.getTransacciones().contiene(transaccion)) {
            billetera.getTransacciones().agregar(transaccion);
        }
    }

    private void registrarEnUsuario(BilleteraDigital billetera, Transaccion transaccion) {
        if (billetera == null || billetera.getUsuario() == null) return;
        Usuario usuario = billetera.getUsuario();
        if (!usuario.getTransacciones().contiene(transaccion)) {
            usuario.getTransacciones().agregar(transaccion);
        }
    }

    private void actualizarSaldoUsuarios(BilleteraDigital origen, BilleteraDigital destino) {
        if (origen != null) actualizarSaldoUsuario(origen.getUsuario());
        if (destino != null && (origen == null || !mismoUsuario(origen, destino))) {
            actualizarSaldoUsuario(destino.getUsuario());
        }
    }

    private void actualizarSaldoUsuario(Usuario usuario) {
        BigDecimal saldo = BigDecimal.ZERO;
        for (BilleteraDigital billetera : usuario.getBilleteras()) {
            saldo = saldo.add(billetera.getSaldoDisponible());
        }
        usuario.setSaldo(saldo);
    }

    private void validarBilleteraActiva(BilleteraDigital billetera) {
        validarBilleteraRegistrada(billetera);
        if (billetera.getEstadoCuenta() != EstadoCuenta.ACTIVO) {
            throw new IllegalStateException("La billetera no está activa");
        }
    }

    private void validarBilleteraRegistrada(BilleteraDigital billetera) {
        Objects.requireNonNull(billetera, "La billetera no puede ser null");
        if (billetera.getCodigo() == null) {
            throw new NoSuchElementException("La billetera no está registrada");
        }
        BilleteraDigital registrada = billeteraService.buscarBilletera(billetera.getCodigo());
        if (!registrada.getIdBilletera().equals(billetera.getIdBilletera())) {
            throw new NoSuchElementException("La billetera no está registrada");
        }
    }

    private void validarUsuarioExistente(BilleteraDigital billetera) {
        Usuario usuario = Objects.requireNonNull(billetera.getUsuario(), "La billetera no tiene usuario");
        Usuario registrado = usuarioService.buscarUsuario(usuario.getIdUsuario());
        if (registrado != usuario) {
            throw new NoSuchElementException("El usuario de la billetera no está registrado");
        }
    }

    private BilleteraDigital resolverBilletera(String codigo) {
        return billeteraService.buscarBilletera(codigo);
    }

    private static void validarMonto(BigDecimal valor) {
        Objects.requireNonNull(valor, "El monto no puede ser null");
        if (valor.signum() <= 0) throw new IllegalArgumentException("El monto debe ser mayor que cero");
    }

    private static boolean mismoUsuario(BilleteraDigital primero, BilleteraDigital segundo) {
        return primero.getUsuario() != null && segundo.getUsuario() != null
                && primero.getUsuario().getIdUsuario().equals(segundo.getUsuario().getIdUsuario());
    }

    private static String mensajeDominio(RuntimeException causa) {
        String mensaje = causa.getMessage();
        return mensaje == null || mensaje.isBlank() ? "Operación rechazada" : mensaje;
    }

    private static ListaEnlazada<Transaccion> copiar(Iterable<Transaccion> historial) {
        ListaEnlazada<Transaccion> copia = new ListaEnlazada<>();
        for (Transaccion transaccion : historial) copia.agregar(transaccion);
        return copia;
    }
}
