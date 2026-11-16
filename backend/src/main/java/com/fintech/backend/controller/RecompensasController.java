package com.fintech.backend.controller;

import com.fintech.backend.dto.ApiDtos;
import com.fintech.backend.model.Beneficio;
import com.fintech.backend.service.RecompensasService;
import com.fintech.backend.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RecompensasController {

    private final RecompensasService recompensas;
    private final UsuarioService usuarios;

    public RecompensasController(RecompensasService recompensas, UsuarioService usuarios) {
        this.recompensas = recompensas;
        this.usuarios = usuarios;
    }

    @GetMapping("/puntos/ranking")
    public List<ApiDtos.UsuarioResponse> ranking() {
        return ApiMapper.map(recompensas.rankingUsuarios(), ApiMapper::usuario);
    }

    @GetMapping("/puntos/rango")
    public List<ApiDtos.UsuarioResponse> puntosRango(
            @org.springframework.web.bind.annotation.RequestParam int minimo,
            @org.springframework.web.bind.annotation.RequestParam int maximo) {
        return ApiMapper.map(recompensas.usuariosEnRango(minimo, maximo), ApiMapper::usuario);
    }

    @GetMapping("/usuarios/{idUsuario}/puntos")
    public java.util.Map<String, Object> puntos(@PathVariable Long idUsuario) {
        var usuario = usuarios.buscarUsuario(idUsuario);
        return java.util.Map.of("idUsuario", idUsuario, "puntos", usuario.getPuntosAcumulados(),
                "nivel", usuario.getNivel().name());
    }

    @GetMapping("/beneficios")
    public List<ApiDtos.BeneficioResponse> beneficios() {
        return ApiMapper.map(recompensas.catalogoBeneficios(), ApiMapper::beneficio);
    }

    @PostMapping("/beneficios")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.BeneficioResponse crearBeneficio(@Valid @RequestBody ApiDtos.BeneficioRequest request) {
        return ApiMapper.beneficio(recompensas.agregarBeneficio(
                request.nombre(), request.descripcion(), request.costoPuntos()));
    }

    @PostMapping("/usuarios/{idUsuario}/canjes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.CanjeResponse canjear(@PathVariable Long idUsuario,
                                          @Valid @RequestBody ApiDtos.CanjeRequest request) {
        return ApiMapper.canje(recompensas.canjear(idUsuario, request.idBeneficio()));
    }

    @GetMapping("/usuarios/{idUsuario}/canjes")
    public List<ApiDtos.CanjeResponse> canjes(@PathVariable Long idUsuario) {
        return ApiMapper.map(recompensas.historialCanjes(idUsuario), ApiMapper::canje);
    }
}
