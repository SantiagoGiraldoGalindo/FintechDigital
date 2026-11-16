package com.fintech.backend.service;

import com.fintech.backend.enums.NivelUsuario;
import com.fintech.backend.enums.TipoNotificacion;
import com.fintech.backend.model.Beneficio;
import com.fintech.backend.model.Canje;
import com.fintech.backend.model.Recompensas;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.structures.hash.TablaHash;
import com.fintech.backend.structures.list.ListaEnlazada;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Reglas de puntos, ventajas por nivel, catálogo y canje de beneficios. */
public class RecompensasService {

    private final UsuarioService usuarioService;
    private NotificacionService notificacionService;
    private final TablaHash<Long, Beneficio> beneficiosPorId = new TablaHash<>();
    private final ListaEnlazada<Beneficio> catalogo = new ListaEnlazada<>();
    private final ListaEnlazada<Canje> canjes = new ListaEnlazada<>();
    private final TablaHash<ClaveDia, Integer> operacionesPorDia = new TablaHash<>();

    public RecompensasService(UsuarioService usuarioService) {
        this(usuarioService, new NotificacionService(new Reloj()));
    }

    public RecompensasService(UsuarioService usuarioService, NotificacionService notificacionService) {
        this.usuarioService = Objects.requireNonNull(usuarioService, "El servicio de usuarios no puede ser null");
        this.notificacionService = Objects.requireNonNull(
                notificacionService, "El servicio de notificaciones no puede ser null");
    }

    void setNotificacionService(NotificacionService notificacionService) {
        this.notificacionService = Objects.requireNonNull(
                notificacionService, "El servicio de notificaciones no puede ser null");
    }

    public NotificacionService getNotificacionService() {
        return notificacionService;
    }

    /** Registra un beneficio con id único y costo positivo. */
    public Beneficio agregarBeneficio(String nombre, String descripcion, int costoPuntos) {
        Beneficio beneficio = new Beneficio(nombre, descripcion, costoPuntos);
        beneficiosPorId.poner(beneficio.getIdBeneficio(), beneficio);
        catalogo.agregar(beneficio);
        return beneficio;
    }

    /** Devuelve una copia del catálogo. */
    public ListaEnlazada<Beneficio> catalogoBeneficios() {
        ListaEnlazada<Beneficio> copia = new ListaEnlazada<>();
        for (Beneficio beneficio : catalogo) copia.agregar(beneficio);
        return copia;
    }

    /**
     * Comprueba que el usuario no exceda su límite diario. Se contabilizan
     * únicamente operaciones completadas; un rechazo no consume cupo.
     */
    public void validarOperacion(Usuario usuario, LocalDateTime fecha) {
        Objects.requireNonNull(usuario, "El usuario no puede ser null");
        Objects.requireNonNull(fecha, "La fecha no puede ser null");
        ClaveDia clave = new ClaveDia(usuario.getIdUsuario(), fecha.toLocalDate());
        int usadas = operacionesPorDia.obtener(clave) == null ? 0 : operacionesPorDia.obtener(clave);
        int limite = VentajasNivel.limiteTransaccionesDiarias(usuario.getNivel());
        if (usadas >= limite) {
            throw new IllegalStateException("Se alcanzó el límite diario de transacciones para el nivel "
                    + usuario.getNivel());
        }
    }

    /**
     * Aplica puntos base y bono del nivel a una operación ya validada,
     * registrando los puntos en la transacción y actualizando usuario/AVL.
     * La recarga recompensa al propietario de destino; retiros y transferencias
     * recompensan al propietario de origen.
     */
    public int aplicarOperacion(Usuario usuario, Transaccion transaccion) {
        int puntos = calcularPuntosOperacion(usuario, transaccion);
        aplicarOperacion(usuario, transaccion, puntos);
        return puntos;
    }

    /** Calcula y valida puntos antes de confirmar los cambios monetarios. */
    public int calcularPuntosOperacion(Usuario usuario, Transaccion transaccion) {
        Objects.requireNonNull(usuario, "El usuario no puede ser null");
        Objects.requireNonNull(transaccion, "La transacción no puede ser null");
        if (!usuarioService.buscarUsuario(usuario.getIdUsuario()).equals(usuario)) {
            throw new NoSuchElementException("El usuario no está registrado");
        }
        Recompensas reglas = recompensasDe(usuario);
        int puntosBase = reglas.calcularPuntos(transaccion);
        int bono = VentajasNivel.bonoPuntos(usuario.getNivel());
        int puntosGanados;
        try {
            puntosGanados = Math.addExact(puntosBase, bono);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Los puntos de la operación exceden el rango permitido", exception);
        }
        try {
            Math.addExact(usuario.getPuntosAcumulados(), puntosGanados);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("El total de puntos excede el rango permitido", exception);
        }
        return puntosGanados;
    }

    /** Confirma una operación previamente validada y cuenta su cupo diario. */
    public void aplicarOperacion(Usuario usuario, Transaccion transaccion, int puntosGanados) {
        Objects.requireNonNull(usuario, "El usuario no puede ser null");
        Objects.requireNonNull(transaccion, "La transacción no puede ser null");
        if (puntosGanados < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos");
        NivelUsuario nivelAnterior = usuario.getNivel();
        Usuario actualizado = usuarioService.ajustarPuntos(usuario.getIdUsuario(), puntosGanados);
        transaccion.setPuntosGenerados(puntosGanados);
        notificarAscensoSiCorresponde(actualizado, nivelAnterior);
        ClaveDia clave = new ClaveDia(usuario.getIdUsuario(), transaccion.getFecha().toLocalDate());
        Integer usadas = operacionesPorDia.obtener(clave);
        operacionesPorDia.poner(clave, usadas == null ? 1 : usadas + 1);
    }

    /**
     * Canjea un beneficio si el usuario tiene puntos suficientes. El canje se
     * almacena globalmente y en el historial enlazado del usuario.
     */
    public Canje canjear(Long idUsuario, Long idBeneficio, LocalDateTime fecha) {
        Usuario usuario = usuarioService.buscarUsuario(idUsuario);
        Objects.requireNonNull(idBeneficio, "El id del beneficio no puede ser null");
        Objects.requireNonNull(fecha, "La fecha del canje no puede ser null");
        Beneficio beneficio = beneficiosPorId.obtener(idBeneficio);
        if (beneficio == null) {
            throw new NoSuchElementException("No existe el beneficio con id " + idBeneficio);
        }
        if (usuario.getPuntosAcumulados() < beneficio.getCostoPuntos()) {
            throw new IllegalStateException("El usuario no tiene puntos suficientes para el canje");
        }

        Canje canje = new Canje(usuario, beneficio, fecha);
        NivelUsuario nivelAnterior = usuario.getNivel();
        usuario = usuarioService.ajustarPuntos(idUsuario, -beneficio.getCostoPuntos());
        Recompensas recompensas = recompensasDe(usuario);
        recompensas.getCanjes().agregar(canje);
        recompensas.getBeneficiosCanjeados().agregar(beneficio.getNombre());
        canjes.agregar(canje);
        notificacionService.notificar(usuario, TipoNotificacion.CANJE_BENEFICIO,
                "Canje realizado: " + beneficio.getNombre());
        notificarAscensoSiCorresponde(usuario, nivelAnterior);
        return canje;
    }

    public Canje canjear(Long idUsuario, Long idBeneficio) {
        return canjear(idUsuario, idBeneficio, LocalDateTime.now());
    }

    public ListaEnlazada<Canje> historialCanjes(Long idUsuario) {
        Usuario usuario = usuarioService.buscarUsuario(idUsuario);
        ListaEnlazada<Canje> copia = new ListaEnlazada<>();
        for (Recompensas recompensas : usuario.getRecompensas()) {
            for (Canje canje : recompensas.getCanjes()) copia.agregar(canje);
        }
        return copia;
    }

    /** Devuelve una copia del historial global de canjes. */
    public ListaEnlazada<Canje> historialCanjes() {
        ListaEnlazada<Canje> copia = new ListaEnlazada<>();
        for (Canje canje : canjes) copia.agregar(canje);
        return copia;
    }

    public ListaEnlazada<Usuario> rankingUsuarios() {
        return usuarioService.listarPorPuntosDescendente();
    }

    public ListaEnlazada<Usuario> usuariosEnRango(int minimo, int maximo) {
        return usuarioService.buscarPorRangoDePuntos(minimo, maximo);
    }

    public int operacionesCompletadasHoy(Long idUsuario, LocalDate fecha) {
        usuarioService.buscarUsuario(idUsuario);
        Objects.requireNonNull(fecha, "La fecha no puede ser null");
        Integer cantidad = operacionesPorDia.obtener(new ClaveDia(idUsuario, fecha));
        return cantidad == null ? 0 : cantidad;
    }

    private Recompensas recompensasDe(Usuario usuario) {
        for (Recompensas recompensas : usuario.getRecompensas()) {
            if (recompensas.getUsuario() == usuario) return recompensas;
        }
        Recompensas nuevas = new Recompensas(usuario);
        usuario.getRecompensas().agregar(nuevas);
        return nuevas;
    }

    private void notificarAscensoSiCorresponde(Usuario usuario, NivelUsuario nivelAnterior) {
        if (usuario.getNivel().ordinal() > nivelAnterior.ordinal()) {
            notificacionService.notificar(usuario, TipoNotificacion.ASCENSO_NIVEL,
                    "Has ascendido al nivel " + usuario.getNivel());
        }
    }

    private static final class ClaveDia {
        private final Long idUsuario;
        private final LocalDate fecha;

        private ClaveDia(Long idUsuario, LocalDate fecha) {
            this.idUsuario = idUsuario;
            this.fecha = fecha;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof ClaveDia clave
                    && idUsuario.equals(clave.idUsuario)
                    && fecha.equals(clave.fecha);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idUsuario, fecha);
        }
    }
}
