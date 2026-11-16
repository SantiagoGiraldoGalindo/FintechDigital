package com.fintech.backend.controller;

import com.fintech.backend.dto.ApiDtos;
import com.fintech.backend.model.Alerta;
import com.fintech.backend.model.Beneficio;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.Canje;
import com.fintech.backend.model.Notificacion;
import com.fintech.backend.model.OperacionProgramada;
import com.fintech.backend.model.RevertirOperacion;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.structures.list.ListaEnlazada;
import java.util.ArrayList;
import java.util.List;

final class ApiMapper {

    private ApiMapper() {
    }

    static ApiDtos.UsuarioResponse usuario(Usuario usuario) {
        return new ApiDtos.UsuarioResponse(usuario.getIdUsuario(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getSaldo(), usuario.getPuntosAcumulados(), usuario.getNivel().name(),
                usuario.getEstado().name());
    }

    static ApiDtos.BilleteraResponse billetera(BilleteraDigital billetera) {
        return new ApiDtos.BilleteraResponse(billetera.getIdBilletera(),
                billetera.getUsuario().getIdUsuario(), billetera.getCodigo(), billetera.getNombre(),
                billetera.getSaldoDisponible(), billetera.getEstadoCuenta().name(),
                billetera.getTipoBilletera());
    }

    static ApiDtos.TransaccionResponse transaccion(Transaccion transaccion) {
        return new ApiDtos.TransaccionResponse(transaccion.getIdTransaccion(),
                transaccion.getTipoTransaccion().name(), transaccion.getValor(), transaccion.getFecha(),
                transaccion.getEstado().name(),
                transaccion.getBilleteraOrigen() == null ? null : transaccion.getBilleteraOrigen().getIdBilletera(),
                transaccion.getBilleteraDestino() == null ? null : transaccion.getBilleteraDestino().getIdBilletera(),
                transaccion.getPuntosGenerados(), transaccion.isDuplicada(),
                transaccion.getNivelRiesgo() == null ? null : transaccion.getNivelRiesgo().name());
    }

    static ApiDtos.OperacionProgramadaResponse programada(OperacionProgramada operacion) {
        return new ApiDtos.OperacionProgramadaResponse(operacion.getIdOperacion(),
                operacion.getTipoOperacion().name(), operacion.getHoraProgramada(),
                operacion.getEstado().name(),
                operacion.getBilleteraOrigen() == null ? null : operacion.getBilleteraOrigen().getCodigo(),
                operacion.getBilleteraDestino().getCodigo(), operacion.getMonto(),
                operacion.getPeriodicidad());
    }

    static ApiDtos.BeneficioResponse beneficio(Beneficio beneficio) {
        return new ApiDtos.BeneficioResponse(beneficio.getIdBeneficio(), beneficio.getNombre(),
                beneficio.getDescripcion(), beneficio.getCostoPuntos());
    }

    static ApiDtos.CanjeResponse canje(Canje canje) {
        return new ApiDtos.CanjeResponse(canje.getIdCanje(), canje.getUsuario().getIdUsuario(),
                canje.getBeneficio().getIdBeneficio(), canje.getBeneficio().getNombre(),
                canje.getPuntosDescontados(), canje.getFecha());
    }

    static ApiDtos.ReversionResponse reversion(RevertirOperacion reversion) {
        return new ApiDtos.ReversionResponse(reversion.getIdReversion(),
                reversion.getTransaccion().getIdTransaccion(), reversion.getUsuario().getIdUsuario(),
                reversion.getFecha(), reversion.getMotivo());
    }

    static ApiDtos.NotificacionResponse notificacion(Notificacion notificacion) {
        return new ApiDtos.NotificacionResponse(notificacion.getIdNotificacion(),
                notificacion.getTipoNotificacion().name(), notificacion.getMensaje(),
                notificacion.getFecha(), notificacion.getUsuario().getIdUsuario());
    }

    static java.util.Map<String, Object> alerta(Alerta alerta) {
        return java.util.Map.of("id", alerta.getIdAlerta(),
                "idUsuario", alerta.getUsuario().getIdUsuario(),
                "idTransaccion", alerta.getTransaccion() == null ? 0L : alerta.getTransaccion().getIdTransaccion(),
                "fecha", alerta.getFecha(), "tipo", alerta.getTipoAlerta(),
                "riesgo", alerta.getNivelRiesgo().name(), "mensaje", alerta.getMensaje(),
                "reporte", alerta.generarReporteEvento());
    }

    static <T, R> List<R> map(Iterable<T> elementos, java.util.function.Function<T, R> conversor) {
        List<R> resultado = new ArrayList<>();
        for (T elemento : elementos) resultado.add(conversor.apply(elemento));
        return resultado;
    }

    static <T> List<T> lista(ListaEnlazada<T> elementos) {
        return map(elementos, java.util.function.Function.identity());
    }
}
