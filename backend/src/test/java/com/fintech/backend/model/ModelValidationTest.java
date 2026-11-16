package com.fintech.backend.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fintech.backend.enums.EstadoTransaccion;
import com.fintech.backend.enums.NivelRiesgo;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.enums.TipoNotificacion;
import com.fintech.backend.enums.TipoTransaccion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ModelValidationTest {

    @Test
    void generaIdsUnicosParaLasEntidades() {
        assertNotEquals(new Usuario().getIdUsuario(), new Usuario().getIdUsuario());
        assertNotEquals(new BilleteraDigital().getIdBilletera(), new Transaccion().getIdTransaccion());
        assertNotEquals(new OperacionProgramada().getIdOperacion(), new Notificacion().getIdNotificacion());
        assertNotEquals(new Alerta().getIdAlerta(), new RevertirOperacion().getIdReversion());
        assertNotEquals(new Recompensas().getIdRecompensa(), new Servicio().getIdServicio());
        assertNotEquals(new Informe().getIdInforme(), new Usuario().getIdUsuario());
    }

    @Test
    void validaMontosYContadoresNoNegativos() {
        BilleteraDigital billetera = new BilleteraDigital();
        Usuario usuario = new Usuario();
        Transaccion transaccion = new Transaccion();
        OperacionProgramada operacion = new OperacionProgramada();
        Recompensas recompensas = new Recompensas();

        assertThrows(IllegalArgumentException.class,
                () -> billetera.setSaldoDisponible(new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> usuario.setSaldo(BigDecimal.valueOf(-1)));
        assertThrows(IllegalArgumentException.class, () -> usuario.setPuntosAcumulados(-1));
        assertThrows(IllegalArgumentException.class, () -> transaccion.setValor(BigDecimal.valueOf(-1)));
        assertThrows(IllegalArgumentException.class, () -> transaccion.setPuntosGenerados(-1));
        assertThrows(IllegalArgumentException.class,
                () -> operacion.setAhorroAutomaticoSemanal(BigDecimal.valueOf(-1)));
        assertThrows(IllegalArgumentException.class, () -> recompensas.setCostoPuntos(-1));
    }

    @Test
    void validaCamposObligatoriosYTiposDeEnumeracion() {
        Usuario usuario = new Usuario("Ana");
        Transaccion transaccion = new Transaccion(BigDecimal.TEN, TipoTransaccion.RETIRO, null, null);
        assertThrows(NullPointerException.class, () -> usuario.setNombre(null));
        assertThrows(NullPointerException.class, () -> usuario.setNivel(null));
        assertThrows(NullPointerException.class, () -> usuario.setBilleteras(null));
        assertThrows(NullPointerException.class, () -> usuario.setTransacciones(null));
        assertThrows(NullPointerException.class, () -> usuario.setNotificaciones(null));
        assertThrows(NullPointerException.class, () -> usuario.setAlertas(null));
        assertThrows(NullPointerException.class, () -> usuario.setOperacionesProgramadas(null));
        assertThrows(NullPointerException.class, () -> usuario.setRecompensas(null));
        assertThrows(NullPointerException.class, () -> usuario.setInformes(null));
        assertThrows(NullPointerException.class, () -> transaccion.setValor(null));
        assertThrows(NullPointerException.class, () -> transaccion.setEstado(null));
        assertThrows(NullPointerException.class, () -> transaccion.setTipoTransaccion(null));
        assertThrows(NullPointerException.class, () -> transaccion.setFecha(null));
        assertThrows(NullPointerException.class, () -> new BilleteraDigital("A", null, usuario));
        assertThrows(NullPointerException.class, () -> new BilleteraDigital("A", TipoBilletera.AHORRO, null));
        assertThrows(NullPointerException.class, () -> new Notificacion("mensaje", null, usuario));
        assertThrows(NullPointerException.class,
                () -> new Notificacion("mensaje", TipoNotificacion.SALDO_BAJO, null));

        Notificacion notificacion = new Notificacion("mensaje", TipoNotificacion.SALDO_BAJO, usuario);
        assertThrows(NullPointerException.class, () -> notificacion.setFecha(null));
        assertThrows(NullPointerException.class, () -> notificacion.setMensaje(null));
        assertThrows(NullPointerException.class, () -> notificacion.setUsuario(null));
        assertThrows(NullPointerException.class, () -> notificacion.setTipoNotificacion(null));
        assertThrows(NullPointerException.class, () -> new Alerta(null, null, null, "alerta", NivelRiesgo.BAJO, "riesgo"));
        assertThrows(NullPointerException.class,
                () -> new Alerta(null, usuario, null, null, NivelRiesgo.BAJO, "riesgo"));
        assertThrows(NullPointerException.class,
                () -> new Alerta(null, usuario, null, "alerta", null, "riesgo"));
        assertThrows(NullPointerException.class,
                () -> new Alerta(null, usuario, null, "alerta", NivelRiesgo.BAJO, null));
        assertThrows(NullPointerException.class, () -> new OperacionProgramada(transaccion, null, EstadoTransaccion.PENDIENTE));
        assertThrows(NullPointerException.class, () -> new OperacionProgramada(transaccion, LocalDateTime.now(), null));
        assertThrows(NullPointerException.class, () -> new OperacionProgramada(null, LocalDateTime.now(), EstadoTransaccion.PENDIENTE));
        assertThrows(NullPointerException.class, () -> new RevertirOperacion(null, usuario, null, "motivo"));
        assertThrows(NullPointerException.class, () -> new RevertirOperacion(transaccion, null, null, "motivo"));
        assertThrows(NullPointerException.class, () -> new RevertirOperacion(transaccion, usuario, null, null));
        assertThrows(NullPointerException.class, () -> new Recompensas(null));
        assertThrows(NullPointerException.class, () -> new Servicio(null, new BilleteraDigital("A", TipoBilletera.AHORRO, usuario)));
        assertThrows(NullPointerException.class, () -> new Servicio(usuario, null));
        Informe informe = new Informe();
        assertThrows(NullPointerException.class, () -> informe.setNombre(null));
        assertThrows(NullPointerException.class, () -> informe.setFecha(null));
        assertThrows(NullPointerException.class, () -> informe.setContenido(null));
        assertThrows(NullPointerException.class, () -> informe.setTipoInforme(null));
        assertThrows(NullPointerException.class, () -> informe.setUsuario(null));
        assertThrows(NullPointerException.class, () -> informe.setTransacciones(null));
    }

    @Test
    void metodosDeNegocioAplazadosDeclaranElPasoPendiente() {
        Usuario usuario = new Usuario("Ana");
        Transaccion transaccion = new Transaccion(BigDecimal.ONE, TipoTransaccion.RECARGA, null, null);
        Servicio servicio = new Servicio(usuario, new BilleteraDigital("A", TipoBilletera.AHORRO, usuario));
        assertThrows(IllegalStateException.class, () -> servicio.realizarRecarga(BigDecimal.ONE));
        assertThrows(UnsupportedOperationException.class, () -> new RevertirOperacion(
                transaccion, usuario, null, "motivo").revertirOperacion());
        assertEquals(0, new Recompensas(usuario).calcularPuntos(transaccion));
        assertEquals(com.fintech.backend.enums.NivelUsuario.BRONCE,
                new Recompensas(usuario).calcularNivel(1));
        Alerta alerta = new Alerta(transaccion, usuario, null, "alerta", NivelRiesgo.BAJO, "tipo");
        alerta.generarAlerta();
        assertTrue(usuario.getAlertas().contiene(alerta));
        alerta.registrarEvento("evento");
        assertTrue(alerta.generarReporteEvento().contains("evento"));
        assertTrue(new Informe("informe", "general", usuario)
                .generarInforme(new com.fintech.backend.structures.list.ListaEnlazada<>())
                .contains("Transacciones: 0"));
    }
}
