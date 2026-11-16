package com.fintech.backend.controller;

import com.fintech.backend.dto.ApiDtos;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<ApiDtos.UsuarioResponse> listar() {
        return ApiMapper.map(usuarios.listarPorPuntos(), ApiMapper::usuario);
    }

    @GetMapping("/{id}")
    public ApiDtos.UsuarioResponse buscar(@PathVariable Long id) {
        return ApiMapper.usuario(usuarios.buscarUsuario(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.UsuarioResponse registrar(@Valid @RequestBody ApiDtos.UsuarioRequest request) {
        return ApiMapper.usuario(usuarios.registrarUsuario(request.nombre(), request.correo()));
    }

    @PutMapping("/{id}")
    public ApiDtos.UsuarioResponse modificar(@PathVariable Long id,
                                              @Valid @RequestBody ApiDtos.UsuarioUpdateRequest request) {
        Usuario usuario = usuarios.modificarUsuario(id, request.nombre(), request.correo(), request.puntos());
        return ApiMapper.usuario(usuario);
    }

    @DeleteMapping("/{id}")
    public ApiDtos.UsuarioResponse eliminar(@PathVariable Long id) {
        return ApiMapper.usuario(usuarios.eliminarUsuario(id));
    }
}
