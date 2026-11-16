package com.fintech.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fintech.backend.enums.EstadoCuenta;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.Usuario;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class BilleteraServiceTest {

    @Test
    void administraVariasBilleterasYDerivaSaldoTotalDelUsuario() {
        UsuarioService usuarios = new UsuarioService();
        BilleteraService service = new BilleteraService(usuarios);
        Usuario usuario = usuarios.registrarUsuario("Ana", "ana@example.com");
        BilleteraDigital ahorro = service.crearBilletera(usuario.getIdUsuario(),
                "ah-1", "Ahorro", TipoBilletera.AHORRO);
        BilleteraDigital gastos = service.crearBilletera(usuario.getIdUsuario(),
                "gs-1", "Gastos", TipoBilletera.GASTOS_DIARIOS);

        assertEquals(2, service.billeterasDeUsuario(usuario.getIdUsuario()).size());
        assertEquals(2, service.cantidadBilleteras());
        assertEquals(ahorro, service.buscarBilletera("AH-1"));

        service.actualizarSaldo("AH-1", new BigDecimal("125.50"));
        service.actualizarSaldo("GS-1", new BigDecimal("24.50"));
        assertEquals(new BigDecimal("125.50"), service.consultarSaldo("AH-1"));
        assertEquals(new BigDecimal("150.00"), usuario.getSaldo());

        service.cambiarEstado("ah-1", EstadoCuenta.BLOQUEADO);
        assertEquals(EstadoCuenta.BLOQUEADO, ahorro.getEstadoCuenta());
        service.modificarBilletera("GS-1", "Gastos diarios", TipoBilletera.COMPRAS);
        assertEquals("Gastos diarios", gastos.getNombre());
        assertEquals(TipoBilletera.COMPRAS, gastos.getTipoBilletera());
        service.cambiarCodigo("GS-1", "GS-2");
        assertEquals(gastos, service.buscarBilletera("gs-2"));
    }

    @Test
    void validaCodigoUsuarioSaldoYEliminacionSegura() {
        UsuarioService usuarios = new UsuarioService();
        BilleteraService service = new BilleteraService(usuarios);
        Usuario usuario = usuarios.registrarUsuario("Ana", "ana@example.com");
        BilleteraDigital billetera = service.crearBilletera(usuario.getIdUsuario(),
                "AH-1", "Ahorro", TipoBilletera.AHORRO);

        assertThrows(IllegalArgumentException.class,
                () -> service.crearBilletera(usuario.getIdUsuario(), "ah-1", "Otra", TipoBilletera.AHORRO));
        assertThrows(NoSuchElementException.class,
                () -> service.crearBilletera(-1L, "OTRA", "Otra", TipoBilletera.AHORRO));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearBilletera(usuario.getIdUsuario(), " ", "Otra", TipoBilletera.AHORRO));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearBilletera(usuario.getIdUsuario(), "OTRA", " ", TipoBilletera.AHORRO));
        assertThrows(NullPointerException.class,
                () -> service.crearBilletera(usuario.getIdUsuario(), "OTRA", "Otra", null));
        assertThrows(NoSuchElementException.class, () -> service.buscarBilletera("NO-EXISTE"));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarSaldo("AH-1", BigDecimal.valueOf(-1)));
        service.actualizarSaldo("AH-1", BigDecimal.ONE);
        assertThrows(IllegalStateException.class, () -> service.eliminarBilletera("AH-1"));
        assertEquals(billetera, service.buscarBilletera("AH-1"));

        service.actualizarSaldo("AH-1", BigDecimal.ZERO);
        service.eliminarBilletera("AH-1");
        assertEquals(0, service.cantidadBilleteras());
        assertEquals(0, usuario.getBilleteras().size());
        assertEquals(BigDecimal.ZERO, usuario.getSaldo());
        assertEquals(usuario, usuarios.eliminarUsuario(usuario.getIdUsuario()));
    }

    @Test
    void noPermiteUsarCodigoExistenteAlCambiarCodigo() {
        UsuarioService usuarios = new UsuarioService();
        BilleteraService service = new BilleteraService(usuarios);
        Usuario usuario = usuarios.registrarUsuario("Ana", "ana@example.com");
        service.crearBilletera(usuario.getIdUsuario(), "AH-1", "Ahorro", TipoBilletera.AHORRO);
        service.crearBilletera(usuario.getIdUsuario(), "GS-1", "Gastos", TipoBilletera.GASTOS_DIARIOS);

        assertThrows(IllegalArgumentException.class, () -> service.cambiarCodigo("AH-1", "GS-1"));
        assertThrows(NullPointerException.class, () -> service.cambiarEstado("AH-1", null));
        assertThrows(NoSuchElementException.class, () -> service.cambiarEstado("NO-EXISTE", EstadoCuenta.ACTIVO));
    }
}
