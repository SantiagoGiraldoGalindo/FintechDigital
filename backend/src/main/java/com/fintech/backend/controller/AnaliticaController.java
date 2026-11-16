package com.fintech.backend.controller;

import com.fintech.backend.dto.ApiDtos;
import com.fintech.backend.model.Informe;
import com.fintech.backend.service.AnaliticaService;
import com.fintech.backend.service.TransaccionService;
import com.fintech.backend.service.UsuarioService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
public class AnaliticaController {

    private final AnaliticaService analitica;
    private final UsuarioService usuarios;
    private final TransaccionService transacciones;

    public AnaliticaController(AnaliticaService analitica, UsuarioService usuarios,
                               TransaccionService transacciones) {
        this.analitica = analitica;
        this.usuarios = usuarios;
        this.transacciones = transacciones;
    }

    @GetMapping("/analitica")
    public Map<String, Object> analitica(@RequestParam LocalDateTime desde,
                                         @RequestParam LocalDateTime hasta,
                                         @RequestParam(defaultValue = "10") int k) {
        String informe = analitica.generarInforme(desde, hasta, k);
        var activo = analitica.usuarioConMayorActividad(desde, hasta);
        return Map.of("desde", desde, "hasta", hasta, "montoTotal", analitica.montoTotal(desde, hasta),
                "usuarioMayorActividad", activo == null ? Map.of() : Map.of(
                        "id", activo.elemento().getIdUsuario(),
                        "nombre", activo.elemento().getNombre(), "operaciones", activo.cantidad()),
                "informe", informe);
    }

    @PostMapping("/informes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> generarInforme(@Valid @RequestBody ApiDtos.SimulacionInformeRequest request) {
        Informe informe = new Informe("Informe analítico", "ANALITICA", null);
        String contenido = informe.generarInforme(
                analitica, request.desde(), request.hasta(), request.k());
        return Map.of("id", informe.getIdInforme(), "fecha", informe.getFecha(),
                "tipo", informe.getTipoInforme(), "contenido", contenido,
                "transacciones", ApiMapper.map(informe.getTransacciones(), ApiMapper::transaccion));
    }

    @GetMapping("/usuarios/{idUsuario}/alertas")
    public List<Map<String, Object>> alertasUsuario(@PathVariable Long idUsuario) {
        return ApiMapper.map(usuarios.buscarUsuario(idUsuario).getAlertas(), ApiMapper::alerta);
    }

    @GetMapping("/auditoria")
    public List<Map<String, Object>> auditoria(@RequestParam LocalDateTime desde, @RequestParam LocalDateTime hasta) {
        return ApiMapper.map(analitica.historialAuditoria(desde, hasta), evento -> Map.of(
                "fecha", evento.getFecha(), "regla", evento.getRegla(),
                "idTransaccion", evento.getTransaccion().getIdTransaccion(),
                "idAlerta", evento.getAlerta().getIdAlerta(),
                "nivelRiesgo", evento.getAlerta().getNivelRiesgo().name()));
    }

    @GetMapping("/grafo/{tipo}/vertices")
    public List<Long> vertices(@PathVariable String tipo) {
        var grafo = grafo(tipo);
        return ApiMapper.lista(grafo.vertices());
    }

    @GetMapping("/grafo/{tipo}/aristas")
    public List<ApiDtos.AristaResponse> aristas(@PathVariable String tipo, @RequestParam(defaultValue = "100") int limite) {
        if (limite < 0) throw new IllegalArgumentException("limite no puede ser negativo");
        var grafo = grafo(tipo);
        return ApiMapper.map(grafo.aristasMasFrecuentes(limite), arista ->
                new ApiDtos.AristaResponse(arista.getOrigen(), arista.getDestino(),
                        arista.getMontoTotal(), arista.getVeces()));
    }

    @GetMapping("/grafo/{tipo}/ciclos")
    public List<List<Long>> ciclos(@PathVariable String tipo) {
        return ApiMapper.map(grafo(tipo).detectarCiclos(), ApiMapper::lista);
    }

    @GetMapping("/grafo/{tipo}/rutas-frecuentes")
    public List<ApiDtos.AristaResponse> rutasFrecuentes(@PathVariable String tipo,
            @RequestParam(defaultValue = "2") int minimoVeces) {
        return ApiMapper.map(grafo(tipo).rutasFrecuentes(minimoVeces), arista ->
                new ApiDtos.AristaResponse(arista.getOrigen(), arista.getDestino(),
                        arista.getMontoTotal(), arista.getVeces()));
    }

    private com.fintech.backend.structures.graph.GrafoDirigido<Long> grafo(String tipo) {
        return switch (tipo.toLowerCase(java.util.Locale.ROOT)) {
            case "usuarios" -> transacciones.getGrafoUsuarios();
            case "billeteras" -> transacciones.getGrafoBilleteras();
            default -> throw new IllegalArgumentException("El tipo debe ser usuarios o billeteras");
        };
    }
}
