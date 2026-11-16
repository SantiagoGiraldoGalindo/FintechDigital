package com.fintech.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.enums.TipoNotificacion;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.Notificacion;
import com.fintech.backend.model.Usuario;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificacionServiceTest {

    private UsuarioService usuarios;
    private BilleteraService billeteras;
    private Reloj reloj;
    private TransaccionService transacciones;
    private Usuario usuario;
    private BilleteraDigital billetera;

    @BeforeEach
    void preparar() {
        usuarios = new UsuarioService();
        billeteras = new BilleteraService(usuarios);
        reloj = new Reloj();
        reloj.fijar(LocalDateTime.of(2026, 11, 12, 8, 0));
        transacciones = new TransaccionService(usuarios, billeteras, reloj);
        usuario = usuarios.registrarUsuario("Ana", "ana@example.com");
        billetera = billeteras.crearBilletera(
                usuario.getIdUsuario(), "ANA", "Principal", TipoBilletera.AHORRO);
    }

    @Test
    void registraRechazoSaldoBajoYDespachaEnOrden() {
        NotificacionService servicio = transacciones.getNotificacionService();
        servicio.setUmbralSaldoBajo(new BigDecimal("10"));
        billeteras.actualizarSaldo("ANA", new BigDecimal("20"));

        assertThrows(OperacionRechazadaException.class,
                () -> transacciones.realizarRetiro(billetera, new BigDecimal("25")));
        transacciones.realizarRetiro(billetera, new BigDecimal("15"));

        assertEquals(2, servicio.cantidadPendientes());
        var despachadas = servicio.despacharPendientes();
        assertEquals(TipoNotificacion.OPERACION_RECHAZADA,
                despachadas.obtener(0).getTipoNotificacion());
        assertEquals(TipoNotificacion.SALDO_BAJO,
                despachadas.obtener(1).getTipoNotificacion());
        assertEquals(TipoNotificacion.OPERACION_RECHAZADA,
                usuario.getNotificacionesRecientes().frente().getTipoNotificacion());
        usuario.getNotificacionesRecientes().desencolar();
        assertEquals(TipoNotificacion.SALDO_BAJO,
                usuario.getNotificacionesRecientes().frente().getTipoNotificacion());
        assertEquals(0, servicio.cantidadPendientes());
    }

    @Test
    void notificaAscensoDeNivelYCanje() {
        usuarios.modificarUsuario(usuario.getIdUsuario(), "Ana", "ana@example.com", 500);
        transacciones.realizarRecarga(billetera, new BigDecimal("100"));
        assertEquals(1, usuario.getNotificaciones().size());
        assertEquals(TipoNotificacion.ASCENSO_NIVEL,
                usuario.getNotificacionesRecientes().frente().getTipoNotificacion());

        var beneficio = transacciones.getRecompensasService()
                .agregarBeneficio("Descuento", "Beneficio", 1);
        transacciones.getRecompensasService().canjear(usuario.getIdUsuario(), beneficio.getIdBeneficio());
        assertEquals(TipoNotificacion.CANJE_BENEFICIO,
                usuario.getNotificaciones().obtener(1).getTipoNotificacion());
    }

    @Test
    void notificaOperacionProgramadaDentroDeLaVentana() {
        NotificacionService notificaciones = transacciones.getNotificacionService();
        notificaciones.setVentanaProxima(Duration.ofHours(2));
        OperacionProgramadaService programadas = new OperacionProgramadaService(transacciones, reloj);
        programadas.programarRecargaAutomatica(
                billetera, BigDecimal.ONE, reloj.ahora().plusHours(1), Duration.ofDays(1));

        programadas.notificarProximas();
        programadas.notificarProximas();

        assertEquals(1, usuario.getNotificaciones().size());
        Notificacion notificacion = usuario.getNotificacionesRecientes().frente();
        assertEquals(TipoNotificacion.TRANSACCION_PROGRAMADA, notificacion.getTipoNotificacion());
        assertEquals(1, notificaciones.cantidadPendientes());
    }

    @Test
    void colaRecienteConservaSoloLasVeinteMasNuevas() {
        NotificacionService servicio = transacciones.getNotificacionService();
        for (int i = 0; i < 21; i++) {
            servicio.notificar(usuario, TipoNotificacion.SALDO_BAJO, "Notificación " + i);
        }

        assertEquals(NotificacionService.CAPACIDAD_HISTORIAL_RECIENTE,
                usuario.getNotificacionesRecientes().size());
        assertEquals(21, usuario.getNotificaciones().size());
        assertEquals("Notificación 1", usuario.getNotificacionesRecientes().frente().getMensaje());
        assertEquals(21, servicio.despacharPendientes().size());
        assertTrue(servicio.despacharPendientes().estaVacia());
    }

    @Test
    void validaConfiguracionDeUmbralYVentana() {
        NotificacionService servicio = transacciones.getNotificacionService();
        assertThrows(IllegalArgumentException.class,
                () -> servicio.setUmbralSaldoBajo(BigDecimal.valueOf(-1)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.setVentanaProxima(Duration.ofSeconds(-1)));
    }
}
