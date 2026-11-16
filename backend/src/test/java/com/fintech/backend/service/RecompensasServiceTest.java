package com.fintech.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fintech.backend.enums.NivelUsuario;
import com.fintech.backend.enums.TipoTransaccion;
import com.fintech.backend.model.Beneficio;
import com.fintech.backend.model.Canje;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RecompensasServiceTest {

    @Test
    void calculaPuntosPorTipoYBloquesCompletosDeCien() {
        com.fintech.backend.model.Recompensas reglas = new com.fintech.backend.model.Recompensas();
        assertEquals(2, reglas.calcularPuntos(transaccion(TipoTransaccion.RECARGA, "299.99")));
        assertEquals(4, reglas.calcularPuntos(transaccion(TipoTransaccion.RETIRO, "250")));
        assertEquals(6, reglas.calcularPuntos(transaccion(TipoTransaccion.TRANSFERENCIA, "250")));
        assertEquals(0, reglas.calcularPuntos(transaccion(TipoTransaccion.PAGO_PROGRAMADO, "1000")));
        assertEquals(NivelUsuario.BRONCE, reglas.calcularNivel(500));
        assertEquals(NivelUsuario.PLATA, reglas.calcularNivel(501));
        assertEquals(NivelUsuario.ORO, reglas.calcularNivel(1_001));
        assertEquals(NivelUsuario.PLATINO, reglas.calcularNivel(5_001));
    }

    @Test
    void aplicaPuntosBonoAsciendeNivelYReubicaEnRankingYRango() {
        UsuarioService usuarios = new UsuarioService();
        BilleteraService billeteras = new BilleteraService(usuarios);
        Usuario ana = new Usuario("Ana", "ana@example.com");
        ana.setPuntosAcumulados(495);
        usuarios.registrarUsuario(ana);
        Usuario bob = usuarios.registrarUsuario("Bob", "bob@example.com");
        bob = usuarios.ajustarPuntos(bob.getIdUsuario(), 1_500);

        TransaccionService transacciones = new TransaccionService(
                usuarios, billeteras, new Reloj());
        var billetera = billeteras.crearBilletera(ana.getIdUsuario(), "ANA", "Ahorros",
                com.fintech.backend.enums.TipoBilletera.AHORRO);

        Transaccion ascenso = transacciones.realizarRecarga(billetera, new BigDecimal("600"));
        assertEquals(6, ascenso.getPuntosGenerados());
        assertEquals(501, ana.getPuntosAcumulados());
        assertEquals(NivelUsuario.PLATA, ana.getNivel());
        assertEquals(ana, transacciones.getRecompensasService().usuariosEnRango(501, 501).obtener(0));

        Transaccion conBono = transacciones.realizarRecarga(billetera, new BigDecimal("100"));
        assertEquals(6, conBono.getPuntosGenerados());
        assertEquals(507, ana.getPuntosAcumulados());
        assertEquals(bob, transacciones.getRecompensasService().rankingUsuarios().obtener(0));
        assertEquals(ana, transacciones.getRecompensasService().rankingUsuarios().obtener(1));
    }

    @Test
    void catalogoCanjeaDescuentaPuntosYGuardaHistorial() {
        UsuarioService usuarios = new UsuarioService();
        Usuario usuario = new Usuario("Ana", "ana@example.com");
        usuario.setPuntosAcumulados(700);
        usuarios.registrarUsuario(usuario);
        RecompensasService service = new RecompensasService(usuarios);
        Beneficio beneficio = service.agregarBeneficio("Descuento", "Descuento en compras", 250);

        assertEquals(1, service.catalogoBeneficios().size());
        Canje canje = service.canjear(usuario.getIdUsuario(), beneficio.getIdBeneficio(),
                LocalDateTime.of(2026, 11, 12, 10, 0));
        assertEquals(250, canje.getPuntosDescontados());
        assertEquals(450, usuario.getPuntosAcumulados());
        assertEquals(NivelUsuario.BRONCE, usuario.getNivel());
        assertEquals(1, service.historialCanjes(usuario.getIdUsuario()).size());
        assertEquals(1, usuario.getRecompensas().obtener(0).getCanjes().size());
        assertEquals(1, usuario.getRecompensas().obtener(0).getBeneficiosCanjeados().size());
        assertEquals(usuario, service.usuariosEnRango(450, 450).obtener(0));

        service.canjear(usuario.getIdUsuario(), beneficio.getIdBeneficio());
        assertEquals(200, usuario.getPuntosAcumulados());
        assertEquals(2, service.historialCanjes(usuario.getIdUsuario()).size());
        assertThrows(IllegalStateException.class,
                () -> service.canjear(usuario.getIdUsuario(), beneficio.getIdBeneficio()));
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.canjear(usuario.getIdUsuario(), -1L));
    }

    @Test
    void aplicaLimiteDiarioPorNivelYRestableceAlDiaSiguiente() {
        UsuarioService usuarios = new UsuarioService();
        BilleteraService billeteras = new BilleteraService(usuarios);
        Usuario usuario = usuarios.registrarUsuario("Ana", "ana@example.com");
        var billetera = billeteras.crearBilletera(usuario.getIdUsuario(), "ANA", "Ahorros",
                com.fintech.backend.enums.TipoBilletera.AHORRO);
        Reloj reloj = new Reloj();
        LocalDateTime hoy = LocalDateTime.of(2026, 11, 12, 10, 0);
        reloj.fijar(hoy);
        TransaccionService transacciones = new TransaccionService(usuarios, billeteras, reloj);

        for (int i = 0; i < VentajasNivel.BRONCE_LIMITE_DIARIO; i++) {
            transacciones.realizarRecarga(billetera, BigDecimal.ONE);
        }
        assertEquals(VentajasNivel.BRONCE_LIMITE_DIARIO,
                transacciones.getRecompensasService().operacionesCompletadasHoy(
                        usuario.getIdUsuario(), hoy.toLocalDate()));

        OperacionRechazadaException rechazo = assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRecarga(billetera, BigDecimal.ONE));
        assertEquals("FALLIDA", rechazo.getTransaccion().getEstado().name());
        assertEquals(VentajasNivel.BRONCE_LIMITE_DIARIO,
                transacciones.getRecompensasService().operacionesCompletadasHoy(
                        usuario.getIdUsuario(), hoy.toLocalDate()));

        reloj.avanzar(java.time.Duration.ofDays(1));
        transacciones.realizarRecarga(billetera, BigDecimal.ONE);
        assertEquals(1, transacciones.getRecompensasService().operacionesCompletadasHoy(
                usuario.getIdUsuario(), LocalDate.of(2026, 11, 13)));
    }

    private static Transaccion transaccion(TipoTransaccion tipo, String valor) {
        return new Transaccion(new BigDecimal(valor), tipo, null, null);
    }
}
