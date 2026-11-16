package com.fintech.backend.controller;

import com.fintech.backend.dto.ApiDtos;
import com.fintech.backend.model.Transaccion;
import com.fintech.backend.service.TransaccionService;
import com.fintech.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReversionController {

    private final TransaccionService transacciones;
    private final UsuarioService usuarios;

    public ReversionController(TransaccionService transacciones, UsuarioService usuarios) {
        this.transacciones = transacciones;
        this.usuarios = usuarios;
    }

    @PostMapping("/reversiones")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.ReversionResponse revertir(@Valid @RequestBody ApiDtos.ReversionRequest request) {
        Transaccion transaccion = null;
        for (Transaccion item : transacciones.historialGlobal()) {
            if (item.getIdTransaccion().equals(request.idTransaccion())) {
                transaccion = item;
                break;
            }
        }
        if (transaccion == null) {
            throw new java.util.NoSuchElementException(
                    "No existe la transacción con id " + request.idTransaccion());
        }
        return ApiMapper.reversion(transacciones.revertirOperacion(transaccion, request.motivo()));
    }

    @org.springframework.web.bind.annotation.GetMapping("/usuarios/{idUsuario}/reversiones")
    public java.util.List<ApiDtos.ReversionResponse> historial(@PathVariable Long idUsuario) {
        return ApiMapper.map(transacciones.historialReversiones(idUsuario), ApiMapper::reversion);
    }
}
