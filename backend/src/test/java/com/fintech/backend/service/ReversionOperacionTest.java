package com.fintech.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fintech.backend.enums.EstadoTransaccion;
import com.fintech.backend.enums.NivelUsuario;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.RevertirOperacion;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReversionOperacionTest {

    private UsuarioService usuarios;
    private BilleteraService billeteras;
    private Reloj reloj;
    private TransaccionService transacciones;
    private Usuario ana;
    private Usuario bob;
    private BilleteraDigital anaWallet;
    private BilleteraDigital bobWallet;

    @BeforeEach
    void preparar() {
        usuarios = new UsuarioService();
        billeteras = new BilleteraService(usuarios);
        reloj = new Reloj();
        reloj.fijar(LocalDateTime.of(2026, 11, 12, 12, 0));
        transacciones = new TransaccionService(usuarios, billeteras, reloj);
        ana = usuarios.registrarUsuario("Ana", "ana@example.com");
        bob = usuarios.registrarUsuario("Bob", "bob@example.com");
        anaWallet = billeteras.crearBilletera(
                ana.getIdUsuario(), "ANA", "Ana", TipoBilletera.AHORRO);
        bobWallet = billeteras.crearBilletera(
                bob.getIdUsuario(), "BOB", "Bob", TipoBilletera.AHORRO);
    }

    @Test
    void revierteRecargaUsaPuntosGuardadosYReubicaUsuarioAlBajarDeNivel() {
        usuarios.modificarUsuario(ana.getIdUsuario(), "Ana", "ana@example.com", 500);
        Transaccion recarga = transacciones.realizarRecarga(anaWallet, new BigDecimal("100"));
        assertEquals(501, ana.getPuntosAcumulados());
        assertEquals(NivelUsuario.PLATA, ana.getNivel());
        assertEquals(1, recarga.getPuntosGenerados());

        RevertirOperacion reversion = transacciones.revertirOperacion(recarga, "Recarga aplicada por error");

        assertEquals(EstadoTransaccion.REVERTIDA, recarga.getEstado());
        assertEquals(new BigDecimal("0"), anaWallet.getSaldoDisponible());
        assertEquals(500, ana.getPuntosAcumulados());
        assertEquals(NivelUsuario.BRONCE, ana.getNivel());
        assertTrue(usuarios.buscarPorRangoDePuntos(500, 500).contiene(ana));
        assertEquals(1, ana.getHistorialReversiones().size());
        assertEquals(reversion, transacciones.historialReversiones(ana.getIdUsuario()).obtener(0));
        assertEquals(recarga, reversion.getTransaccion());
    }

    @Test
    void revierteTransferenciaYDescuentaMontoYFrecuenciaDeGrafos() {
        billeteras.actualizarSaldo("ANA", new BigDecimal("50"));
        Transaccion transferencia = transacciones.realizarTransaccion(
                anaWallet, bobWallet, new BigDecimal("20"));
        Transaccion segunda = transacciones.realizarTransaccion(
                anaWallet, bobWallet, new BigDecimal("15"));
        assertEquals(new BigDecimal("35"), bobWallet.getSaldoDisponible());
        assertEquals(1, transacciones.getGrafoBilleteras().rutasFrecuentes(1).size());

        transacciones.revertirOperacion(segunda, "Transferencia equivocada");

        var aristaRestante = transacciones.getGrafoBilleteras().rutasFrecuentes(1).obtener(0);
        assertEquals(new BigDecimal("20"), aristaRestante.getMontoTotal());
        assertEquals(1, aristaRestante.getVeces());
        assertEquals(new BigDecimal("30"), anaWallet.getSaldoDisponible());
        assertEquals(new BigDecimal("20"), bobWallet.getSaldoDisponible());

        transacciones.revertirOperacion(transferencia, "Transferencia anterior equivocada");
        assertEquals(new BigDecimal("50"), anaWallet.getSaldoDisponible());
        assertEquals(new BigDecimal("0"), bobWallet.getSaldoDisponible());
        assertTrue(transacciones.getGrafoBilleteras().rutasFrecuentes(1).estaVacia());
        assertTrue(transacciones.getGrafoUsuarios().rutasFrecuentes(1).estaVacia());
        assertEquals(EstadoTransaccion.REVERTIDA, transferencia.getEstado());
    }

    @Test
    void rechazaDobleReversionYReversionFueraDelOrdenLifo() {
        Transaccion anterior = transacciones.realizarRecarga(anaWallet, BigDecimal.TEN);
        Transaccion reciente = transacciones.realizarRecarga(anaWallet, BigDecimal.ONE);

        assertThrows(IllegalStateException.class,
                () -> transacciones.revertirOperacion(anterior, "Fuera de orden"));
        transacciones.revertirOperacion(reciente, "Duplicada");
        assertThrows(IllegalStateException.class,
                () -> transacciones.revertirOperacion(reciente, "Segundo intento"));
        transacciones.revertirOperacion(anterior, "Deshacer anterior");
        assertEquals(2, transacciones.historialReversiones(ana.getIdUsuario()).size());
    }

    @Test
    void saldoInsuficienteEnDestinoNoCambiaLaOperacionNiLaSacaDeLaPila() {
        Transaccion recarga = transacciones.realizarRecarga(anaWallet, new BigDecimal("20"));
        billeteras.actualizarSaldo("ANA", new BigDecimal("5"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> transacciones.revertirOperacion(recarga, "No hay saldo"));

        assertTrue(error.getMessage().contains("saldo suficiente"));
        assertEquals(EstadoTransaccion.COMPLETADA, recarga.getEstado());
        assertEquals(1, ana.getOperacionesReversibles().size());
        assertTrue(ana.getHistorialReversiones().estaVacia());
        billeteras.actualizarSaldo("ANA", new BigDecimal("20"));
        transacciones.revertirOperacion(recarga, "Saldo repuesto");
        assertEquals(BigDecimal.ZERO, anaWallet.getSaldoDisponible());
    }

    @Test
    void detectaDuplicadosDentroDeCincoMinutosYRechazaRetirosComoReversibles() {
        billeteras.actualizarSaldo("ANA", new BigDecimal("30"));
        Transaccion primera = transacciones.realizarTransaccion(anaWallet, bobWallet, BigDecimal.TEN);
        reloj.avanzar(Duration.ofMinutes(5));
        Transaccion fueraDeVentana = transacciones.realizarTransaccion(anaWallet, bobWallet, BigDecimal.TEN);
        assertFalse(primera.isDuplicada());
        assertTrue(fueraDeVentana.isDuplicada());

        Transaccion retiro = transacciones.realizarRetiro(bobWallet, BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class,
                () -> transacciones.revertirOperacion(retiro, "Los retiros no se revierten"));
    }

    @Test
    void pagoProgramadoSoloSePuedeRevertirCuandoEsDuplicado() {
        billeteras.actualizarSaldo("ANA", new BigDecimal("20"));
        Transaccion pagoOriginal = transacciones.realizarPagoProgramado(
                anaWallet, bobWallet, new BigDecimal("5"));
        Transaccion pagoDuplicado = transacciones.realizarPagoProgramado(
                anaWallet, bobWallet, new BigDecimal("5"));

        assertTrue(pagoDuplicado.isDuplicada());
        assertThrows(IllegalArgumentException.class,
                () -> transacciones.revertirOperacion(pagoOriginal, "Pago único"));
        transacciones.revertirOperacion(pagoDuplicado, "Pago duplicado");
        assertEquals(new BigDecimal("5"), bobWallet.getSaldoDisponible());
        assertEquals(EstadoTransaccion.REVERTIDA, pagoDuplicado.getEstado());
    }
}
