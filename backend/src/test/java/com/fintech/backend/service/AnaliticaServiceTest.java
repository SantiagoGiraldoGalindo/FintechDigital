package com.fintech.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fintech.backend.enums.NivelRiesgo;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.enums.TipoTransaccion;
import com.fintech.backend.model.Alerta;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.EventoAuditoria;
import com.fintech.backend.model.Informe;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnaliticaServiceTest {

    private UsuarioService usuarios;
    private BilleteraService billeteras;
    private Reloj reloj;
    private TransaccionService transacciones;
    private AnaliticaService analitica;
    private Usuario ana;
    private Usuario bob;
    private BilleteraDigital anaA;
    private BilleteraDigital anaB;
    private BilleteraDigital bobA;
    private LocalDateTime inicio;

    @BeforeEach
    void preparar() {
        usuarios = new UsuarioService();
        billeteras = new BilleteraService(usuarios);
        reloj = new Reloj();
        inicio = LocalDateTime.of(2026, 11, 12, 9, 0);
        reloj.fijar(inicio);
        transacciones = new TransaccionService(usuarios, billeteras, reloj);
        analitica = transacciones.getAnaliticaService();
        ana = crearUsuario("Ana", "ana@example.com");
        bob = crearUsuario("Bob", "bob@example.com");
        anaA = crearBilletera(ana, "ANA-A", TipoBilletera.AHORRO);
        anaB = crearBilletera(ana, "ANA-B", TipoBilletera.GASTOS_DIARIOS);
        bobA = crearBilletera(bob, "BOB-A", TipoBilletera.COMPRAS);
    }

    @Test
    void detectaCadaUnaDeLasSeisReglasDeRiesgo() {
        billeteras.actualizarSaldo("ANA-A", new BigDecimal("1000"));
        transacciones.realizarTransaccion(anaA, bobA, BigDecimal.TEN);
        transacciones.realizarTransaccion(anaA, bobA, BigDecimal.TEN);
        Transaccion tercera = transacciones.realizarTransaccion(anaA, bobA, BigDecimal.TEN);
        assertTrue(tieneRegla(tercera, "TRANSFERENCIAS_SEGUIDAS"));
        assertTrue(tieneRegla(tercera, "REPETICION_MISMO_DESTINO"));
        assertEquals(NivelRiesgo.ALTO, tercera.getNivelRiesgo());

        usuarios.modificarUsuario(ana.getIdUsuario(), "Ana", ana.getCorreo(), 6_000);
        transacciones.realizarRecarga(anaA, BigDecimal.TEN);
        Transaccion montoInusual = transacciones.realizarRecarga(anaA, new BigDecimal("100"));
        assertTrue(tieneRegla(montoInusual, "MONTO_SUPERIOR_PROMEDIO"));

        BilleteraDigital anaC = crearBilletera(ana, "ANA-C", TipoBilletera.TRANSPORTE);
        Usuario carlos = crearUsuario("Carlos", "carlos@example.com");
        BilleteraDigital carlosA = crearBilletera(carlos, "CARLOS-A", TipoBilletera.COMPRAS);
        billeteras.actualizarSaldo("ANA-B", BigDecimal.TEN);
        billeteras.actualizarSaldo("ANA-C", BigDecimal.TEN);
        transacciones.realizarTransaccion(anaA, bobA, BigDecimal.ONE);
        transacciones.realizarTransaccion(anaB, bobA, BigDecimal.ONE);
        Transaccion fragmentada = transacciones.realizarTransaccion(anaC, carlosA, BigDecimal.ONE);
        assertTrue(tieneRegla(fragmentada, "FRAGMENTACION_MULTIBILLETERA"));

        reloj.fijar(inicio);
        Usuario frecuencia = crearUsuario("Frecuencia", "frecuencia@example.com");
        BilleteraDigital wallet = crearBilletera(frecuencia, "FREC", TipoBilletera.AHORRO);
        BilleteraDigital target = crearBilletera(bob, "BOB-B", TipoBilletera.COMPRAS);
        billeteras.actualizarSaldo("FREC", new BigDecimal("1000"));
        for (int i = 0; i < 4; i++) {
            transacciones.realizarRecarga(wallet, BigDecimal.ONE);
            reloj.avanzar(Duration.ofHours(2));
        }
        transacciones.realizarTransaccion(wallet, target, BigDecimal.ONE);
        transacciones.realizarTransaccion(wallet, target, BigDecimal.ONE);
        transacciones.realizarTransaccion(wallet, target, BigDecimal.ONE);
        Transaccion cambioFrecuencia = transacciones.realizarTransaccion(wallet, target, BigDecimal.ONE);
        assertTrue(tieneRegla(cambioFrecuencia, "CAMBIO_BRUSCO_FRECUENCIA"));

        Usuario horario = crearUsuario("Horario", "horario@example.com");
        BilleteraDigital horarioWallet = crearBilletera(horario, "HORARIO", TipoBilletera.AHORRO);
        billeteras.actualizarSaldo("HORARIO", new BigDecimal("100"));
        reloj.fijar(inicio);
        for (int i = 0; i < 3; i++) {
            transacciones.realizarRecarga(horarioWallet, BigDecimal.TEN);
            reloj.avanzar(Duration.ofHours(1));
        }
        reloj.fijar(inicio.plusDays(1).withHour(2));
        Transaccion nocturna = transacciones.realizarRecarga(horarioWallet, BigDecimal.TEN);
        assertTrue(tieneRegla(nocturna, "HORARIO_NO_HABITUAL"));
    }

    @Test
    void ofreceMetricasRangosTopKGrafoEInformeConsolidado() {
        billeteras.actualizarSaldo("ANA-A", new BigDecimal("1000"));
        Transaccion aBob = transacciones.realizarTransaccion(anaA, bobA, new BigDecimal("30"));
        reloj.avanzar(Duration.ofMinutes(1));
        Transaccion deBob = transacciones.realizarTransaccion(bobA, anaA, new BigDecimal("20"));
        reloj.avanzar(Duration.ofMinutes(1));
        Transaccion segundaABob = transacciones.realizarTransaccion(anaA, bobA, new BigDecimal("40"));
        LocalDateTime fin = reloj.ahora();

        assertEquals(new BigDecimal("90"), analitica.montoTotal(inicio, fin));
        assertEquals(3, analitica.frecuenciaPorTipo().obtener(TipoTransaccion.TRANSFERENCIA));
        assertEquals(1, analitica.topTransaccionesPorValor(inicio, fin, 1).size());
        assertEquals(segundaABob, analitica.topTransaccionesPorValor(inicio, fin, 1).obtener(0));
        assertEquals(1, analitica.ciclosEntreUsuarios().size());
        assertEquals(1, analitica.rutasFrecuentesEntreUsuarios(2).size());
        assertEquals(3, analitica.billeterasConMayorUso().obtener(0).cantidad());
        assertEquals(3, analitica.usuariosConMasTransferencias().obtener(0).cantidad());
        assertFalse(analitica.categoriasMasActivas().estaVacia());
        assertEquals(3, analitica.usuarioConMayorActividad(inicio, fin).cantidad());
        assertEquals(1, analitica.historialAuditoria(inicio, fin).size());

        Informe informe = new Informe("Informe mensual", "ANALITICA", ana);
        String contenido = informe.generarInforme(analitica, inicio, fin, 2);
        assertTrue(contenido.contains("Monto total"));
        assertTrue(contenido.contains("Frecuencia por tipo"));
        assertTrue(contenido.contains("Ciclos entre usuarios"));
        assertEquals(3, informe.getTransacciones().size());
        assertEquals(aBob.getValor().add(deBob.getValor()).add(segundaABob.getValor()),
                analitica.montoTotal(inicio, fin));
    }

    @Test
    void generaAlertaYEventoDeAuditoriaConsultableParaUnaAnomalia() {
        billeteras.actualizarSaldo("ANA-A", new BigDecimal("1000"));
        transacciones.realizarTransaccion(anaA, bobA, BigDecimal.ONE);
        transacciones.realizarTransaccion(anaA, bobA, BigDecimal.ONE);
        Transaccion anomalous = transacciones.realizarTransaccion(anaA, bobA, BigDecimal.ONE);

        assertNotNull(anomalous.getNivelRiesgo());
        assertFalse(ana.getAlertas().estaVacia());
        assertFalse(analitica.historialAuditoria().estaVacia());
        EventoAuditoria evento = analitica.historialAuditoria().obtener(0);
        assertEquals(anomalous, evento.getTransaccion());
        assertEquals(evento.getRegla(), evento.getAlerta().getTipoAlerta());
        assertTrue(evento.getAlerta().generarReporteEvento().contains(evento.getRegla()));
    }

    private boolean tieneRegla(Transaccion transaccion, String regla) {
        for (EventoAuditoria evento : analitica.historialAuditoria()) {
            if (evento.getTransaccion() == transaccion && evento.getRegla().equals(regla)) return true;
        }
        return false;
    }

    private Usuario crearUsuario(String nombre, String correo) {
        Usuario usuario = new Usuario(nombre, correo);
        usuario.setPuntosAcumulados(6_000);
        return usuarios.registrarUsuario(usuario);
    }

    private BilleteraDigital crearBilletera(Usuario usuario, String codigo, TipoBilletera tipo) {
        return billeteras.crearBilletera(usuario.getIdUsuario(), codigo, codigo, tipo);
    }
}
