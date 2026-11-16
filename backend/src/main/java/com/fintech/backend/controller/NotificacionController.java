package com.fintech.backend.controller;

import com.fintech.backend.dto.ApiDtos;
import com.fintech.backend.service.NotificacionService;
import com.fintech.backend.service.UsuarioService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificaciones;
    private final UsuarioService usuarios;

    public NotificacionController(NotificacionService notificaciones, UsuarioService usuarios) {
        this.notificaciones = notificaciones;
        this.usuarios = usuarios;
    }

    @GetMapping("/usuarios/{idUsuario}")
    public List<ApiDtos.NotificacionResponse> recientes(@PathVariable Long idUsuario) {
        return ApiMapper.map(notificaciones.notificacionesRecientes(
                usuarios.buscarUsuario(idUsuario)), ApiMapper::notificacion);
    }

    @GetMapping("/pendientes")
    public java.util.Map<String, Integer> pendientes() {
        return java.util.Map.of("cantidad", notificaciones.cantidadPendientes());
    }

    @PostMapping("/despachar")
    public List<ApiDtos.NotificacionResponse> despachar() {
        return ApiMapper.map(notificaciones.despacharPendientes(), ApiMapper::notificacion);
    }
}
