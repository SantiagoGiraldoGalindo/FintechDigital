package com.fintech.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fintech.backend.enums.EstadoCuenta;
import com.fintech.backend.enums.EstadoTransaccion;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.Servicio;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransaccionServiceTest {

    private UsuarioService usuarios;
    private BilleteraService billeteras;
    private Reloj reloj;
    private TransaccionService transacciones;
    private Usuario ana;
    private Usuario bob;
    private BilleteraDigital anaAhorros;
    private BilleteraDigital anaGastos;
    private BilleteraDigital bobAhorros;

    @BeforeEach
    void preparar() {
        usuarios = new UsuarioService();
        billeteras = new BilleteraService(usuarios);
        reloj = new Reloj();
        reloj.fijar(LocalDateTime.of(2026, 11, 12, 12, 0));
        transacciones = new TransaccionService(usuarios, billeteras, reloj);

        ana = usuarios.registrarUsuario("Ana", "ana@example.com");
        bob = usuarios.registrarUsuario("Bob", "bob@example.com");
        anaAhorros = billeteras.crearBilletera(ana.getIdUsuario(), "ANA-A", "Ahorros", TipoBilletera.AHORRO);
        anaGastos = billeteras.crearBilletera(ana.getIdUsuario(), "ANA-G", "Gastos", TipoBilletera.GASTOS_DIARIOS);
        bobAhorros = billeteras.crearBilletera(bob.getIdUsuario(), "BOB-A", "Ahorros", TipoBilletera.AHORRO);
    }

    @Test
    void recargaRegistraTransaccionYActualizaHistorialesYSaldos() {
        Servicio servicio = new Servicio(transacciones);
        servicio.setBilletera(anaAhorros);
        Transaccion recarga = servicio.realizarRecarga(new BigDecimal("100.25"));

        assertEquals(EstadoTransaccion.COMPLETADA, recarga.getEstado());
        assertEquals(1, recarga.getPuntosGenerados());
        assertEquals(LocalDateTime.of(2026, 11, 12, 12, 0), recarga.getFecha());
        assertEquals(new BigDecimal("100.25"), transacciones.consultarSaldo(anaAhorros));
        assertEquals(new BigDecimal("100.25"), ana.getSaldo());
        assertEquals(1, transacciones.historialGlobal().size());
        assertEquals(1, transacciones.historialPorUsuario(ana.getIdUsuario()).size());
        assertEquals(1, transacciones.historialPorBilletera("ANA-A").size());
        assertEquals(0, transacciones.historialPorBilletera("ANA-G").size());
    }

    @Test
    void retiroYTransferenciasEntreBilleterasPropiasYDeOtrosUsuarios() {
        transacciones.realizarRecarga(anaAhorros, new BigDecimal("200"));
        Transaccion transferenciaPropia = transacciones.realizarTransaccion(
                anaAhorros, anaGastos, new BigDecimal("30"));
        Transaccion transferenciaExterna = transacciones.realizarTransaccion(
                anaAhorros, bobAhorros, new BigDecimal("70"));
        Transaccion retiro = transacciones.realizarRetiro(anaGastos, new BigDecimal("10"));

        assertEquals(EstadoTransaccion.COMPLETADA, transferenciaPropia.getEstado());
        assertEquals(EstadoTransaccion.COMPLETADA, transferenciaExterna.getEstado());
        assertEquals(EstadoTransaccion.COMPLETADA, retiro.getEstado());
        assertEquals(new BigDecimal("100"), anaAhorros.getSaldoDisponible());
        assertEquals(new BigDecimal("20"), anaGastos.getSaldoDisponible());
        assertEquals(new BigDecimal("70"), bobAhorros.getSaldoDisponible());
        assertEquals(new BigDecimal("120"), ana.getSaldo());
        assertEquals(new BigDecimal("70"), bob.getSaldo());

        assertEquals(4, transacciones.historialGlobal().size());
        assertEquals(4, transacciones.historialPorUsuario(ana.getIdUsuario()).size());
        assertEquals(1, transacciones.historialPorUsuario(bob.getIdUsuario()).size());
        assertEquals(3, transacciones.historialPorBilletera("ANA-A").size());
        assertEquals(2, transacciones.historialPorBilletera("ANA-G").size());
        assertEquals(1, transacciones.historialPorBilletera("BOB-A").size());

        assertTrue(transacciones.getGrafoUsuarios().existeCamino(ana.getIdUsuario(), bob.getIdUsuario()));
        assertTrue(transacciones.getGrafoBilleteras().existeCamino(
                anaAhorros.getIdBilletera(), bobAhorros.getIdBilletera()));
        assertEquals(2, transacciones.getGrafoBilleteras()
                .aristasMasFrecuentes(10).size());
    }

    @Test
    void filtraHistorialPorRangoInclusivoUsandoReloj() {
        LocalDateTime inicio = reloj.ahora();
        transacciones.realizarRecarga(anaAhorros, BigDecimal.TEN);
        reloj.avanzar(java.time.Duration.ofHours(1));
        transacciones.realizarRecarga(anaAhorros, BigDecimal.TEN);
        reloj.avanzar(java.time.Duration.ofHours(1));
        transacciones.realizarRecarga(anaAhorros, BigDecimal.TEN);

        assertEquals(2, transacciones.historialPorRango(
                inicio, inicio.plusHours(1)).size());
        assertEquals(3, transacciones.historialPorRango(
                inicio, inicio.plusHours(2)).size());
        assertThrows(IllegalArgumentException.class, () -> transacciones.historialPorRango(
                inicio.plusHours(1), inicio));
        assertThrows(NullPointerException.class, () -> transacciones.historialPorRango(null, inicio));
    }

    @Test
    void rechazaMontoInvalidoYRegistraLaTransaccionFallida() {
        OperacionRechazadaException cero = assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRecarga(anaAhorros, BigDecimal.ZERO));
        OperacionRechazadaException negativo = assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRetiro(anaAhorros, BigDecimal.valueOf(-2)));
        OperacionRechazadaException nulo = assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRecarga(anaAhorros, null));

        assertTrue(cero.getMessage().contains("mayor que cero"));
        assertEquals(EstadoTransaccion.FALLIDA, cero.getTransaccion().getEstado());
        assertEquals(EstadoTransaccion.FALLIDA, negativo.getTransaccion().getEstado());
        assertEquals(EstadoTransaccion.FALLIDA, nulo.getTransaccion().getEstado());
        assertEquals(3, transacciones.cantidadTransacciones());
        assertEquals(3, transacciones.historialPorUsuario(ana.getIdUsuario()).size());
        assertEquals(BigDecimal.ZERO, anaAhorros.getSaldoDisponible());
    }

    @Test
    void rechazaBilleterasInactivasSaldoInsuficienteMismaBilleteraYNoRegistradas() {
        billeteras.actualizarSaldo("ANA-A", BigDecimal.TEN);
        anaGastos.setEstadoCuenta(EstadoCuenta.BLOQUEADO);

        OperacionRechazadaException inactiva = assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRecarga(anaGastos, BigDecimal.ONE));
        OperacionRechazadaException fondos = assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRetiro(anaAhorros, new BigDecimal("11")));
        OperacionRechazadaException misma = assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarTransaccion(anaAhorros, anaAhorros, BigDecimal.ONE));
        OperacionRechazadaException noRegistrada = assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRecarga(new BilleteraDigital(
                        "NO-REG", "No registrada", TipoBilletera.AHORRO, ana), BigDecimal.ONE));

        assertEquals(EstadoTransaccion.FALLIDA, inactiva.getTransaccion().getEstado());
        assertTrue(fondos.getMessage().contains("insuficiente"));
        assertTrue(misma.getMessage().contains("distintas"));
        assertEquals(EstadoTransaccion.FALLIDA, noRegistrada.getTransaccion().getEstado());
        assertEquals(4, transacciones.cantidadTransacciones());
        assertEquals(BigDecimal.TEN, anaAhorros.getSaldoDisponible());
    }

    @Test
    void rechazoPorCodigoDesconocidoQuedaEnElHistorialGlobal() {
        OperacionRechazadaException rechazo = assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRetiro("NO-EXISTE", BigDecimal.ONE));
        assertEquals(EstadoTransaccion.FALLIDA, rechazo.getTransaccion().getEstado());
        assertEquals(1, transacciones.historialGlobal().size());
    }

    @Test
    void rechazaUsuariosNoRegistradosEnBilleteraYServicioFacadeConsultaSaldo() {
        Usuario desconocido = new Usuario("Desconocido", "no@example.com");
        BilleteraDigital ajena = new BilleteraDigital("AJENA", "Ajena", TipoBilletera.AHORRO, desconocido);
        ajena.setSaldoDisponible(BigDecimal.TEN);
        assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRetiro(ajena, BigDecimal.ONE));

        Servicio servicio = new Servicio(transacciones);
        servicio.setBilletera(anaAhorros);
        assertEquals(BigDecimal.ZERO, servicio.consultarSaldo());
        assertEquals(BigDecimal.ZERO, servicio.consultarSaldo(anaAhorros));
        assertFalse(comoLista(transacciones.historialGlobal()).isEmpty());
    }

    private static List<Transaccion> comoLista(Iterable<Transaccion> iterable) {
        List<Transaccion> resultado = new ArrayList<>();
        iterable.forEach(resultado::add);
        return resultado;
    }
}
