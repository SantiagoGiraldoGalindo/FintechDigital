package com.fintech.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fintech.backend.enums.EstadoTransaccion;
import com.fintech.backend.enums.NivelUsuario;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.enums.TipoOperacionProgramada;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.OperacionProgramada;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperacionProgramadaServiceTest {

    private UsuarioService usuarios;
    private BilleteraService billeteras;
    private Reloj reloj;
    private TransaccionService transacciones;
    private OperacionProgramadaService operaciones;
    private LocalDateTime inicio;

    @BeforeEach
    void preparar() {
        usuarios = new UsuarioService();
        billeteras = new BilleteraService(usuarios);
        reloj = new Reloj();
        inicio = LocalDateTime.of(2026, 11, 12, 8, 0);
        reloj.fijar(inicio);
        transacciones = new TransaccionService(usuarios, billeteras, reloj);
        operaciones = new OperacionProgramadaService(transacciones, reloj);
    }

    @Test
    void procesaOperacionesVencidasPorFechaYLuegoPorNivel() {
        Usuario bronce = usuarios.registrarUsuario("Bronce", "bronce@example.com");
        Usuario platino = new Usuario("Platino", "platino@example.com");
        platino.setPuntosAcumulados(6_000);
        usuarios.registrarUsuario(platino);
        BilleteraDigital billeteraBronce = crearBilletera(bronce, "BRONCE");
        BilleteraDigital billeteraPlatino = crearBilletera(platino, "PLATINO");

        OperacionProgramada operacionBronce = operaciones.programarRecargaAutomatica(
                billeteraBronce, BigDecimal.TEN, inicio, Duration.ofDays(1));
        OperacionProgramada operacionPlatino = operaciones.programarRecargaAutomatica(
                billeteraPlatino, BigDecimal.TEN, inicio, Duration.ofDays(1));

        var ejecutadas = operaciones.procesarPendientes();
        assertEquals(2, ejecutadas.size());
        assertEquals(platino.getIdUsuario(),
                ejecutadas.obtener(0).getBilleteraDestino().getUsuario().getIdUsuario());
        assertEquals(bronce.getIdUsuario(),
                ejecutadas.obtener(1).getBilleteraDestino().getUsuario().getIdUsuario());
        assertEquals(EstadoTransaccion.COMPLETADA, operacionBronce.getEstado());
        assertEquals(EstadoTransaccion.COMPLETADA, operacionPlatino.getEstado());
        assertEquals(2, operaciones.cantidadPendientes());
    }

    @Test
    void reprogramaRecurrenciasYProcesaVariosVencimientos() {
        Usuario usuario = usuarios.registrarUsuario("Ana", "ana@example.com");
        BilleteraDigital origen = crearBilletera(usuario, "ORIGEN");
        BilleteraDigital destino = crearBilletera(usuario, "DESTINO");
        billeteras.actualizarSaldo("ORIGEN", new BigDecimal("100"));
        OperacionProgramada plan = operaciones.programarTransferenciaPeriodica(
                origen, destino, BigDecimal.TEN, inicio.plusHours(1), Duration.ofDays(1));

        reloj.avanzar(Duration.ofHours(26));
        var resultados = operaciones.procesarPendientes();

        assertEquals(2, resultados.size());
        assertEquals(new BigDecimal("80"), origen.getSaldoDisponible());
        assertEquals(new BigDecimal("20"), destino.getSaldoDisponible());
        assertEquals(inicio.plusHours(49), plan.getHoraProgramada());
        assertEquals(EstadoTransaccion.COMPLETADA, plan.getEstado());
        assertEquals(2, transacciones.historialPorUsuario(usuario.getIdUsuario()).size());
        assertEquals(1, operaciones.cantidadPendientes());
    }

    @Test
    void pagoProgramadoOtorgaBonoYFalloPorSaldoSeMarcaRechazado() {
        Usuario platino = new Usuario("Platino", "platino@example.com");
        platino.setPuntosAcumulados(6_000);
        usuarios.registrarUsuario(platino);
        Usuario comercio = usuarios.registrarUsuario("Comercio", "comercio@example.com");
        BilleteraDigital origen = crearBilletera(platino, "PLATINO");
        BilleteraDigital destino = crearBilletera(comercio, "COMERCIO");
        billeteras.actualizarSaldo("PLATINO", BigDecimal.TEN);

        OperacionProgramada pago = operaciones.programarPagoRecurrente(
                origen, destino, BigDecimal.ONE, inicio, Duration.ofDays(30));
        Transaccion transaccion = operaciones.procesarPendientes().obtener(0);
        assertEquals(20, transaccion.getPuntosGenerados());
        assertEquals(EstadoTransaccion.COMPLETADA, transaccion.getEstado());
        assertEquals(TipoOperacionProgramada.PAGO_RECURRENTE, pago.getTipoOperacion());

        billeteras.actualizarSaldo("PLATINO", BigDecimal.ZERO);
        OperacionProgramada sinFondos = operaciones.programarPagoRecurrente(
                origen, destino, BigDecimal.ONE, inicio, Duration.ofDays(30));
        Transaccion rechazada = operaciones.procesarPendientes().obtener(0);
        assertEquals(EstadoTransaccion.RECHAZADA, rechazada.getEstado());
        assertEquals(EstadoTransaccion.RECHAZADA, sinFondos.getEstado());
        assertEquals(EstadoTransaccion.RECHAZADA, sinFondos.getTransaccion().getEstado());
        assertEquals(BigDecimal.ZERO, origen.getSaldoDisponible());
    }

    @Test
    void simularAvanceProcesaVencimientosYSemanalUsaSieteDias() {
        Usuario usuario = usuarios.registrarUsuario("Ana", "ana@example.com");
        BilleteraDigital origen = crearBilletera(usuario, "ORIGEN");
        BilleteraDigital destino = crearBilletera(usuario, "DESTINO");
        billeteras.actualizarSaldo("ORIGEN", new BigDecimal("50"));
        OperacionProgramada ahorro = operaciones.programarAhorroAutomaticoSemanal(
                origen, destino, BigDecimal.TEN, inicio.plusDays(7));
        OperacionProgramada recarga = operaciones.programarRecargaAutomatica(
                destino, BigDecimal.ONE, inicio.plusDays(2), Duration.ofDays(2));
        assertEquals(Duration.ofDays(7), ahorro.getPeriodicidad());

        SimulacionService simulacion = new SimulacionService(reloj, operaciones);
        var primera = simulacion.avanzarYProcesar(Duration.ofDays(2));
        assertEquals(1, primera.size());
        assertEquals(EstadoTransaccion.COMPLETADA, recarga.getEstado());
        var segunda = simulacion.avanzarYProcesar(Duration.ofDays(5));
        assertEquals(3, segunda.size());
        assertEquals(EstadoTransaccion.COMPLETADA, ahorro.getEstado());
        assertEquals(inicio.plusDays(14), ahorro.getHoraProgramada());
        assertEquals(inicio.plusDays(8), recarga.getHoraProgramada());
        assertFalse(segunda.estaVacia());
        assertTrue(simulacion.horaActual().isAfter(inicio));
    }

    @Test
    void validaPeriodicidadMontoYBilleterasAlProgramar() {
        Usuario usuario = usuarios.registrarUsuario("Ana", "ana@example.com");
        BilleteraDigital origen = crearBilletera(usuario, "ORIGEN");
        BilleteraDigital destino = crearBilletera(usuario, "DESTINO");
        assertThrows(IllegalArgumentException.class, () -> operaciones.programar(
                TipoOperacionProgramada.TRANSFERENCIA_PERIODICA, origen, destino,
                BigDecimal.ZERO, inicio, Duration.ofDays(1)));
        assertThrows(IllegalArgumentException.class, () -> operaciones.programar(
                TipoOperacionProgramada.RECARGA_AUTOMATICA, null, destino,
                BigDecimal.ONE, inicio, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> operaciones.programar(
                TipoOperacionProgramada.PAGO_RECURRENTE, origen, origen,
                BigDecimal.ONE, inicio, Duration.ofDays(1)));
        assertThrows(NullPointerException.class, () -> operaciones.programarRecargaAutomatica(
                null, BigDecimal.ONE, inicio, Duration.ofDays(1)));
    }

    private BilleteraDigital crearBilletera(Usuario usuario, String codigo) {
        return billeteras.crearBilletera(usuario.getIdUsuario(), codigo, codigo,
                TipoBilletera.AHORRO);
    }
}
