package com.fintech.backend.controller;

import com.fintech.backend.dto.ApiDtos;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.OperacionProgramada;
import com.fintech.backend.service.BilleteraService;
import com.fintech.backend.service.OperacionProgramadaService;
import com.fintech.backend.service.Reloj;
import com.fintech.backend.service.SimulacionService;
import com.fintech.backend.service.TransaccionService;
import com.fintech.backend.service.UsuarioService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OperacionController {

    private final BilleteraService billeteras;
    private final TransaccionService transacciones;
    private final OperacionProgramadaService programadas;
    private final SimulacionService simulacion;
    private final Reloj reloj;
    private final UsuarioService usuarios;

    public OperacionController(BilleteraService billeteras, TransaccionService transacciones,
                               OperacionProgramadaService programadas,
                               SimulacionService simulacion, Reloj reloj, UsuarioService usuarios) {
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.programadas = programadas;
        this.simulacion = simulacion;
        this.reloj = reloj;
        this.usuarios = usuarios;
    }

    @PostMapping("/operaciones/recarga")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.TransaccionResponse recargar(@Valid @RequestBody ApiDtos.RecargaRequest request) {
        return ApiMapper.transaccion(transacciones.realizarRecarga(
                request.codigoDestino(), request.monto()));
    }

    @PostMapping("/operaciones/retiro")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.TransaccionResponse retirar(@Valid @RequestBody ApiDtos.RetiroRequest request) {
        return ApiMapper.transaccion(transacciones.realizarRetiro(
                request.codigoOrigen(), request.monto()));
    }

    @PostMapping("/operaciones/transferencia")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.TransaccionResponse transferir(@Valid @RequestBody ApiDtos.TransferenciaRequest request) {
        return ApiMapper.transaccion(transacciones.realizarTransaccion(
                request.codigoOrigen(), request.codigoDestino(), request.monto()));
    }

    @GetMapping("/historial")
    public List<ApiDtos.TransaccionResponse> historial(
            @RequestParam(required = false) Long idUsuario,
            @RequestParam(required = false) String codigoBilletera,
            @RequestParam(required = false) LocalDateTime desde,
            @RequestParam(required = false) LocalDateTime hasta) {
        if (desde != null || hasta != null) {
            if (desde == null || hasta == null) {
                throw new IllegalArgumentException("desde y hasta deben enviarse juntos");
            }
            return ApiMapper.map(transacciones.historialPorRango(desde, hasta), ApiMapper::transaccion);
        }
        if (idUsuario != null) {
            return ApiMapper.map(transacciones.historialPorUsuario(idUsuario), ApiMapper::transaccion);
        }
        if (codigoBilletera != null) {
            return ApiMapper.map(transacciones.historialPorBilletera(codigoBilletera), ApiMapper::transaccion);
        }
        return ApiMapper.map(transacciones.historialGlobal(), ApiMapper::transaccion);
    }

    @PostMapping("/programadas")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.OperacionProgramadaResponse programar(
            @Valid @RequestBody ApiDtos.ProgramadaRequest request) {
        BilleteraDigital origen = request.codigoOrigen() == null
                ? null : billeteras.buscarBilletera(request.codigoOrigen());
        BilleteraDigital destino = billeteras.buscarBilletera(request.codigoDestino());
        OperacionProgramada operacion = programadas.programar(request.tipo(), origen, destino,
                request.monto(), request.horaProgramada(), request.periodicidad());
        return ApiMapper.programada(operacion);
    }

    @GetMapping("/programadas/{idUsuario}")
    public List<ApiDtos.OperacionProgramadaResponse> programadasDeUsuario(@PathVariable Long idUsuario) {
        return ApiMapper.map(
                usuarios.buscarUsuario(idUsuario).getOperacionesProgramadas(),
                ApiMapper::programada);
    }

    @PostMapping("/simulacion/avanzar")
    public java.util.Map<String, Object> avanzar(@Valid @RequestBody ApiDtos.SimulacionRequest request) {
        var resultado = simulacion.avanzarYProcesar(request.avanzar());
        return java.util.Map.of("horaActual", simulacion.horaActual(),
                "ejecutadas", ApiMapper.map(resultado, ApiMapper::transaccion));
    }

    @GetMapping("/simulacion/hora")
    public LocalDateTime hora() {
        return reloj.ahora();
    }
}
