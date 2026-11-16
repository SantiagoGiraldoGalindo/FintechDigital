package com.fintech.backend.service;

import com.fintech.backend.enums.EstadoTransaccion;
import com.fintech.backend.enums.NivelRiesgo;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.enums.TipoTransaccion;
import com.fintech.backend.model.Alerta;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.EventoAuditoria;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.structures.graph.GrafoDirigido;
import com.fintech.backend.structures.hash.TablaHash;
import com.fintech.backend.structures.list.ListaEnlazada;
import com.fintech.backend.structures.queue.ColaPrioridad;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Consultas analíticas y detección en línea de patrones inusuales. */
public class AnaliticaService {

    private final UsuarioService usuarioService;
    private final TransaccionService transaccionService;
    private final Reloj reloj;
    private final ListaEnlazada<EventoAuditoria> historialAuditoria = new ListaEnlazada<>();
    private final TablaHash<Long, Boolean> transaccionesAnalizadas = new TablaHash<>();

    public AnaliticaService(UsuarioService usuarioService, TransaccionService transaccionService,
                           Reloj reloj) {
        this.usuarioService = Objects.requireNonNull(usuarioService, "El servicio de usuarios no puede ser null");
        this.transaccionService = Objects.requireNonNull(
                transaccionService, "El servicio de transacciones no puede ser null");
        this.reloj = Objects.requireNonNull(reloj, "El reloj no puede ser null");
    }

    /**
     * Analiza una transacción completada una sola vez. Cada regla detectada crea
     * una alerta del usuario titular, establece riesgo máximo y deja evidencia
     * en el historial de auditoría.
     */
    public ListaEnlazada<Alerta> detectarAnomalias(Transaccion transaccion) {
        Objects.requireNonNull(transaccion, "La transacción no puede ser null");
        ListaEnlazada<Alerta> alertas = new ListaEnlazada<>();
        if (transaccion.getEstado() != EstadoTransaccion.COMPLETADA
                || transaccionesAnalizadas.contieneClave(transaccion.getIdTransaccion())) {
            return alertas;
        }
        transaccionesAnalizadas.poner(transaccion.getIdTransaccion(), Boolean.TRUE);
        Usuario usuario = usuarioTitular(transaccion);
        if (usuario == null) return alertas;

        NivelRiesgo mayorRiesgo = null;
        if (transaccionesSeguidas(usuario, transaccion) >= UmbralesDeteccion.TRANSFERENCIAS_SEGUIDAS) {
            mayorRiesgo = agregarAlerta(alertas, transaccion, usuario, "TRANSFERENCIAS_SEGUIDAS",
                    "Varias transferencias se realizaron en poco tiempo", NivelRiesgo.ALTO, billeteraOrigen(transaccion));
        }
        if (montoMuySuperior(usuario, transaccion)) {
            mayorRiesgo = mayor(mayorRiesgo, agregarAlerta(alertas, transaccion, usuario,
                    "MONTO_SUPERIOR_PROMEDIO", "El monto supera significativamente el promedio del usuario",
                    NivelRiesgo.ALTO, billeteraOrigen(transaccion)));
        }
        if (repeticionesMismoDestino(usuario, transaccion)
                >= UmbralesDeteccion.REPETICIONES_MISMO_DESTINO) {
            mayorRiesgo = mayor(mayorRiesgo, agregarAlerta(alertas, transaccion, usuario,
                    "REPETICION_MISMO_DESTINO", "Se repitieron operaciones al mismo destino en poco tiempo",
                    NivelRiesgo.MEDIO, billeteraOrigen(transaccion)));
        }
        if (billeterasDistintasEnVentana(usuario, transaccion)
                >= UmbralesDeteccion.BILLETERAS_PARA_FRAGMENTACION
                && transaccion.getValor().compareTo(UmbralesDeteccion.MONTO_MAXIMO_FRAGMENTADO) <= 0) {
            mayorRiesgo = mayor(mayorRiesgo, agregarAlerta(alertas, transaccion, usuario,
                    "FRAGMENTACION_MULTIBILLETERA", "Se usaron varias billeteras para dividir transferencias",
                    NivelRiesgo.ALTO, billeteraOrigen(transaccion)));
        }
        if (frecuenciaBrusca(usuario, transaccion)) {
            mayorRiesgo = mayor(mayorRiesgo, agregarAlerta(alertas, transaccion, usuario,
                    "CAMBIO_BRUSCO_FRECUENCIA", "La frecuencia actual difiere del historial habitual",
                    NivelRiesgo.MEDIO, billeteraOrigen(transaccion)));
        }
        if (horarioInusual(usuario, transaccion)) {
            mayorRiesgo = mayor(mayorRiesgo, agregarAlerta(alertas, transaccion, usuario,
                    "HORARIO_NO_HABITUAL", "La operación ocurrió fuera del horario habitual del usuario",
                    NivelRiesgo.MEDIO, billeteraOrigen(transaccion)));
        }
        if (mayorRiesgo != null) transaccion.setNivelRiesgo(mayorRiesgo);
        return alertas;
    }

    public ListaEnlazada<EventoAuditoria> historialAuditoria() {
        return copiar(historialAuditoria);
    }

    public ListaEnlazada<EventoAuditoria> historialAuditoria(LocalDateTime desde, LocalDateTime hasta) {
        validarRango(desde, hasta);
        ListaEnlazada<EventoAuditoria> resultado = new ListaEnlazada<>();
        for (EventoAuditoria evento : historialAuditoria) {
            if (!evento.getFecha().isBefore(desde) && !evento.getFecha().isAfter(hasta)) {
                resultado.agregar(evento);
            }
        }
        return resultado;
    }

    public ListaEnlazada<Transaccion> transaccionesEnRango(LocalDateTime desde, LocalDateTime hasta) {
        return transaccionService.historialPorRango(desde, hasta);
    }

    /** Billeteras ordenadas por cantidad de operaciones completadas. */
    public ListaEnlazada<Conteo<BilleteraDigital>> billeterasConMayorUso() {
        ListaEnlazada<Conteo<BilleteraDigital>> conteos = new ListaEnlazada<>();
        for (Usuario usuario : usuarioService.listarPorPuntos()) {
            for (BilleteraDigital billetera : usuario.getBilleteras()) {
                int uso = contarCompletadas(billetera.getTransacciones());
                if (uso > 0) conteos.agregar(new Conteo<>(billetera, uso));
            }
        }
        return ordenarDescendente(conteos);
    }

    /** Usuarios ordenados por transferencias enviadas o recibidas. */
    public ListaEnlazada<Conteo<Usuario>> usuariosConMasTransferencias() {
        ListaEnlazada<Conteo<Usuario>> conteos = new ListaEnlazada<>();
        for (Usuario usuario : usuarioService.listarPorPuntos()) {
            int transferencias = 0;
            for (Transaccion transaccion : usuario.getTransacciones()) {
                if (esTransferencia(transaccion) && transaccion.getEstado() == EstadoTransaccion.COMPLETADA) {
                    transferencias++;
                }
            }
            if (transferencias > 0) conteos.agregar(new Conteo<>(usuario, transferencias));
        }
        return ordenarDescendente(conteos);
    }

    /** Categorías de billetera ordenadas por cantidad de operaciones completadas. */
    public ListaEnlazada<Conteo<TipoBilletera>> categoriasMasActivas() {
        TablaHash<TipoBilletera, Integer> conteos = new TablaHash<>();
        for (Usuario usuario : usuarioService.listarPorPuntos()) {
            for (BilleteraDigital billetera : usuario.getBilleteras()) {
                int cantidad = contarCompletadas(billetera.getTransacciones());
                Integer anterior = conteos.obtener(billetera.getTipoBilletera());
                conteos.poner(billetera.getTipoBilletera(), (anterior == null ? 0 : anterior) + cantidad);
            }
        }
        ListaEnlazada<Conteo<TipoBilletera>> resultado = new ListaEnlazada<>();
        for (TipoBilletera tipo : conteos.claves()) {
            int cantidad = conteos.obtener(tipo);
            if (cantidad > 0) resultado.agregar(new Conteo<>(tipo, cantidad));
        }
        return ordenarDescendente(resultado);
    }

    /** Grafo de patrones entre usuarios producido por las transferencias. */
    public GrafoDirigido<Long> patronesEntreUsuarios() {
        return transaccionService.getGrafoUsuarios();
    }

    /** Frecuencia de transacciones completadas por tipo. */
    public TablaHash<TipoTransaccion, Integer> frecuenciaPorTipo() {
        TablaHash<TipoTransaccion, Integer> frecuencia = new TablaHash<>();
        for (Transaccion transaccion : transaccionService.historialGlobal()) {
            if (transaccion.getEstado() == EstadoTransaccion.COMPLETADA) {
                Integer actual = frecuencia.obtener(transaccion.getTipoTransaccion());
                frecuencia.poner(transaccion.getTipoTransaccion(), actual == null ? 1 : actual + 1);
            }
        }
        return frecuencia;
    }

    /** Suma el valor de transacciones completadas del rango inclusivo. */
    public BigDecimal montoTotal(LocalDateTime desde, LocalDateTime hasta) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaccion transaccion : transaccionesEnRango(desde, hasta)) {
            if (transaccion.getEstado() == EstadoTransaccion.COMPLETADA) {
                total = total.add(transaccion.getValor());
            }
        }
        return total;
    }

    /** Usuario con mayor número de transacciones completadas en el período. */
    public Conteo<Usuario> usuarioConMayorActividad(LocalDateTime desde, LocalDateTime hasta) {
        ListaEnlazada<Transaccion> rango = transaccionesEnRango(desde, hasta);
        Usuario mayor = null;
        int maximo = 0;
        for (Usuario usuario : usuarioService.listarPorPuntos()) {
            int cantidad = 0;
            for (Transaccion transaccion : rango) {
                if (transaccion.getEstado() == EstadoTransaccion.COMPLETADA
                        && participa(usuario, transaccion)) cantidad++;
            }
            if (cantidad > maximo) {
                mayor = usuario;
                maximo = cantidad;
            }
        }
        return mayor == null ? null : new Conteo<>(mayor, maximo);
    }

    /** Top-K transacciones completadas por valor, en una ventana inclusiva. */
    public ListaEnlazada<Transaccion> topTransaccionesPorValor(
            LocalDateTime desde, LocalDateTime hasta, int k) {
        validarRango(desde, hasta);
        if (k < 0) throw new IllegalArgumentException("k no puede ser negativo");
        ColaPrioridad<Transaccion> top = new ColaPrioridad<>(
                Comparator.comparing(Transaccion::getValor).reversed()
                        .thenComparing(Transaccion::getFecha));
        for (Transaccion transaccion : transaccionesEnRango(desde, hasta)) {
            if (transaccion.getEstado() == EstadoTransaccion.COMPLETADA) top.insertar(transaccion);
        }
        ListaEnlazada<Transaccion> resultado = new ListaEnlazada<>();
        while (k > 0 && top.size() > 0) {
            resultado.agregar(top.extraer());
            k--;
        }
        return resultado;
    }

    public ListaEnlazada<ListaEnlazada<Long>> ciclosEntreUsuarios() {
        return patronesEntreUsuarios().detectarCiclos();
    }

    public ListaEnlazada<GrafoDirigido.Arista<Long>> rutasFrecuentesEntreUsuarios(int minimoVeces) {
        return patronesEntreUsuarios().rutasFrecuentes(minimoVeces);
    }

    /** Texto consolidado con métricas, rutas, ciclos, anomalías y top-K. */
    public String generarInforme(LocalDateTime desde, LocalDateTime hasta, int k) {
        validarRango(desde, hasta);
        if (k < 0) throw new IllegalArgumentException("k no puede ser negativo");
        Conteo<Usuario> usuarioActivo = usuarioConMayorActividad(desde, hasta);
        StringBuilder informe = new StringBuilder();
        informe.append("Analítica de ").append(desde).append(" a ").append(hasta).append('\n')
                .append("Monto total: ").append(montoTotal(desde, hasta)).append('\n')
                .append("Usuario con mayor actividad: ")
                .append(usuarioActivo == null ? "sin actividad"
                        : usuarioActivo.elemento().getNombre() + " (" + usuarioActivo.cantidad() + ")")
                .append('\n').append("Frecuencia por tipo: ");
        TablaHash<TipoTransaccion, Integer> frecuencia = frecuenciaPorTipo();
        for (TipoTransaccion tipo : frecuencia.claves()) {
            informe.append(tipo).append('=').append(frecuencia.obtener(tipo)).append(' ');
        }
        informe.append("\nBilleteras más usadas: ");
        for (Conteo<BilleteraDigital> conteo : billeterasConMayorUso()) {
            informe.append(conteo.elemento().getCodigo()).append('=').append(conteo.cantidad()).append(' ');
        }
        informe.append("\nUsuarios con más transferencias: ");
        for (Conteo<Usuario> conteo : usuariosConMasTransferencias()) {
            informe.append(conteo.elemento().getNombre()).append('=').append(conteo.cantidad()).append(' ');
        }
        informe.append("\nCategorías más activas: ");
        for (Conteo<TipoBilletera> conteo : categoriasMasActivas()) {
            informe.append(conteo.elemento()).append('=').append(conteo.cantidad()).append(' ');
        }
        informe.append("\nPatrones entre usuarios: ");
        for (GrafoDirigido.Arista<Long> arista : patronesEntreUsuarios().aristasMasFrecuentes(
                patronesEntreUsuarios().cantidadAristas())) {
            informe.append(arista.getOrigen()).append("->").append(arista.getDestino())
                    .append("(veces=").append(arista.getVeces())
                    .append(", monto=").append(arista.getMontoTotal()).append(") ");
        }
        informe.append("\nCiclos entre usuarios: ");
        for (ListaEnlazada<Long> ciclo : ciclosEntreUsuarios()) {
            informe.append(formatearCiclo(ciclo)).append(' ');
        }
        informe.append("\nRutas frecuentes (2+): ");
        for (GrafoDirigido.Arista<Long> arista : rutasFrecuentesEntreUsuarios(2)) {
            informe.append(arista.getOrigen()).append("->").append(arista.getDestino())
                    .append("(veces=").append(arista.getVeces()).append(") ");
        }
        informe.append("\nAuditoría de riesgo: ");
        for (EventoAuditoria evento : historialAuditoria(desde, hasta)) {
            informe.append(evento.getRegla()).append("(tx=")
                    .append(evento.getTransaccion().getIdTransaccion()).append(") ");
        }
        informe.append("\nTop-K por valor: ");
        for (Transaccion transaccion : topTransaccionesPorValor(desde, hasta, k)) {
            informe.append(transaccion.getIdTransaccion()).append('=')
                    .append(transaccion.getValor()).append(' ');
        }
        return informe.toString();
    }

    private static String formatearCiclo(ListaEnlazada<Long> ciclo) {
        StringBuilder texto = new StringBuilder();
        for (Long usuario : ciclo) {
            if (!texto.isEmpty()) texto.append("->");
            texto.append(usuario);
        }
        return texto.toString();
    }

    private NivelRiesgo agregarAlerta(ListaEnlazada<Alerta> alertas, Transaccion transaccion,
                                      Usuario usuario, String regla, String mensaje,
                                      NivelRiesgo riesgo, BilleteraDigital billetera) {
        Alerta alerta = new Alerta(transaccion, usuario, billetera, mensaje, riesgo, regla);
        alerta.setFecha(reloj.ahora());
        alerta.generarAlerta();
        alertas.agregar(alerta);
        historialAuditoria.agregar(new EventoAuditoria(reloj.ahora(), transaccion, alerta, regla));
        return riesgo;
    }

    private int transaccionesSeguidas(Usuario usuario, Transaccion actual) {
        return contarVentana(usuario, actual, UmbralesDeteccion.VENTANA_TRANSFERENCIAS,
                transaccion -> esTransferencia(transaccion));
    }

    private int repeticionesMismoDestino(Usuario usuario, Transaccion actual) {
        if (!esTransferencia(actual) || actual.getBilleteraDestino() == null) return 0;
        return contarVentana(usuario, actual, UmbralesDeteccion.VENTANA_REPETICION_DESTINO,
                transaccion -> esTransferencia(transaccion)
                        && transaccion.getBilleteraDestino() != null
                        && transaccion.getBilleteraDestino().getIdBilletera()
                                .equals(actual.getBilleteraDestino().getIdBilletera()));
    }

    private int contarVentana(Usuario usuario, Transaccion actual, Duration ventana,
                              java.util.function.Predicate<Transaccion> condicion) {
        int cantidad = condicion.test(actual) ? 1 : 0;
        LocalDateTime desde = actual.getFecha().minus(ventana);
        for (Transaccion otra : usuario.getTransacciones()) {
            if (otra != actual && otra.getEstado() == EstadoTransaccion.COMPLETADA
                    && !otra.getFecha().isBefore(desde) && !otra.getFecha().isAfter(actual.getFecha())
                    && condicion.test(otra)) cantidad++;
        }
        return cantidad;
    }

    private boolean montoMuySuperior(Usuario usuario, Transaccion actual) {
        BigDecimal total = BigDecimal.ZERO;
        int cantidad = 0;
        for (Transaccion otra : usuario.getTransacciones()) {
            if (otra != actual && otra.getEstado() == EstadoTransaccion.COMPLETADA
                    && !otra.getFecha().isAfter(actual.getFecha())) {
                total = total.add(otra.getValor());
                cantidad++;
            }
        }
        return cantidad > 0 && actual.getValor().compareTo(
                total.divide(BigDecimal.valueOf(cantidad), 2, java.math.RoundingMode.HALF_UP)
                        .multiply(UmbralesDeteccion.MULTIPLICADOR_MONTO_PROMEDIO)) > 0;
    }

    private int billeterasDistintasEnVentana(Usuario usuario, Transaccion actual) {
        if (!esTransferencia(actual) || actual.getBilleteraOrigen() == null
                || actual.getValor().compareTo(UmbralesDeteccion.MONTO_MAXIMO_FRAGMENTADO) > 0) return 0;
        Set<Long> billeteras = new HashSet<>();
        billeteras.add(actual.getBilleteraOrigen().getIdBilletera());
        LocalDateTime desde = actual.getFecha().minus(UmbralesDeteccion.VENTANA_FRAGMENTACION);
        for (Transaccion otra : usuario.getTransacciones()) {
            if (otra != actual && otra.getEstado() == EstadoTransaccion.COMPLETADA
                    && esTransferencia(otra) && otra.getBilleteraOrigen() != null
                    && otra.getValor().compareTo(UmbralesDeteccion.MONTO_MAXIMO_FRAGMENTADO) <= 0
                    && !otra.getFecha().isBefore(desde) && !otra.getFecha().isAfter(actual.getFecha())) {
                billeteras.add(otra.getBilleteraOrigen().getIdBilletera());
            }
        }
        return billeteras.size();
    }

    private boolean frecuenciaBrusca(Usuario usuario, Transaccion actual) {
        int anteriores = 0;
        LocalDateTime anterior = null;
        Duration sumaIntervalos = Duration.ZERO;
        for (Transaccion otra : usuario.getTransacciones()) {
            if (otra == actual || otra.getEstado() != EstadoTransaccion.COMPLETADA
                    || otra.getFecha().isAfter(actual.getFecha())) continue;
            anteriores++;
            if (anterior != null && otra.getFecha().isAfter(anterior)) {
                sumaIntervalos = sumaIntervalos.plus(Duration.between(anterior, otra.getFecha()));
            }
            if (anterior == null || otra.getFecha().isAfter(anterior)) anterior = otra.getFecha();
        }
        if (anteriores < 2) return false;
        long intervalos = anteriores - 1L;
        Duration promedio = sumaIntervalos.dividedBy(intervalos);
        return promedio.compareTo(UmbralesDeteccion.INTERVALO_HABITUAL_MINIMO) >= 0
                && contarVentana(usuario, actual, UmbralesDeteccion.VENTANA_FRECUENCIA_BRUSCA,
                        ignored -> true) >= UmbralesDeteccion.OPERACIONES_FRECUENCIA_BRUSCA;
    }

    private boolean horarioInusual(Usuario usuario, Transaccion actual) {
        ListaEnlazada<Transaccion> anteriores = new ListaEnlazada<>();
        for (Transaccion otra : usuario.getTransacciones()) {
            if (otra != actual && otra.getEstado() == EstadoTransaccion.COMPLETADA
                    && otra.getFecha().isBefore(actual.getFecha())) anteriores.agregar(otra);
        }
        if (anteriores.size() < UmbralesDeteccion.HISTORIAL_MINIMO_HORARIO) return false;
        int minutoActual = actual.getFecha().getHour() * 60 + actual.getFecha().getMinute();
        int tolerancia = Math.toIntExact(UmbralesDeteccion.TOLERANCIA_HORARIA.toMinutes());
        for (Transaccion otra : anteriores) {
            int minutoAnterior = otra.getFecha().getHour() * 60 + otra.getFecha().getMinute();
            int diferencia = Math.abs(minutoActual - minutoAnterior);
            if (Math.min(diferencia, 24 * 60 - diferencia) <= tolerancia) return false;
        }
        return true;
    }

    private Usuario usuarioTitular(Transaccion transaccion) {
        BilleteraDigital billetera = transaccion.getBilleteraOrigen() == null
                ? transaccion.getBilleteraDestino() : transaccion.getBilleteraOrigen();
        return billetera == null ? null : billetera.getUsuario();
    }

    private static BilleteraDigital billeteraOrigen(Transaccion transaccion) {
        return transaccion.getBilleteraOrigen() == null
                ? transaccion.getBilleteraDestino() : transaccion.getBilleteraOrigen();
    }

    private static boolean esTransferencia(Transaccion transaccion) {
        return transaccion.getTipoTransaccion() == TipoTransaccion.TRANSFERENCIA
                || transaccion.getTipoTransaccion() == TipoTransaccion.PAGO_PROGRAMADO;
    }

    private static boolean participa(Usuario usuario, Transaccion transaccion) {
        return (transaccion.getBilleteraOrigen() != null
                && transaccion.getBilleteraOrigen().getUsuario().getIdUsuario().equals(usuario.getIdUsuario()))
                || (transaccion.getBilleteraDestino() != null
                && transaccion.getBilleteraDestino().getUsuario().getIdUsuario().equals(usuario.getIdUsuario()));
    }

    private static int contarCompletadas(Iterable<Transaccion> transacciones) {
        int cantidad = 0;
        for (Transaccion transaccion : transacciones) {
            if (transaccion.getEstado() == EstadoTransaccion.COMPLETADA) cantidad++;
        }
        return cantidad;
    }

    private static NivelRiesgo mayor(NivelRiesgo primero, NivelRiesgo segundo) {
        return primero == null || segundo.ordinal() > primero.ordinal() ? segundo : primero;
    }

    private static void validarRango(LocalDateTime desde, LocalDateTime hasta) {
        Objects.requireNonNull(desde, "La fecha inicial no puede ser null");
        Objects.requireNonNull(hasta, "La fecha final no puede ser null");
        if (desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
    }

    private static <T> ListaEnlazada<T> copiar(Iterable<T> elementos) {
        ListaEnlazada<T> copia = new ListaEnlazada<>();
        for (T elemento : elementos) copia.agregar(elemento);
        return copia;
    }

    private static <T> ListaEnlazada<Conteo<T>> ordenarDescendente(
            ListaEnlazada<Conteo<T>> conteos) {
        java.util.ArrayList<Conteo<T>> ordenados = new java.util.ArrayList<>();
        for (Conteo<T> conteo : conteos) ordenados.add(conteo);
        ordenados.sort(Comparator.comparingInt(Conteo<T>::cantidad).reversed());
        ListaEnlazada<Conteo<T>> resultado = new ListaEnlazada<>();
        for (Conteo<T> conteo : ordenados) resultado.agregar(conteo);
        return resultado;
    }

    public record Conteo<T>(T elemento, int cantidad) {
    }
}
