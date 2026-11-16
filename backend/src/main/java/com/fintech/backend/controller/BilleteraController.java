package com.fintech.backend.controller;

import com.fintech.backend.dto.ApiDtos;
import com.fintech.backend.service.BilleteraService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billeteras")
public class BilleteraController {

    private final BilleteraService billeteras;

    public BilleteraController(BilleteraService billeteras) {
        this.billeteras = billeteras;
    }

    @GetMapping("/{codigo}")
    public ApiDtos.BilleteraResponse buscar(@PathVariable String codigo) {
        return ApiMapper.billetera(billeteras.buscarBilletera(codigo));
    }

    @GetMapping
    public List<ApiDtos.BilleteraResponse> deUsuario(@RequestParam Long idUsuario) {
        return ApiMapper.map(billeteras.billeterasDeUsuario(idUsuario), ApiMapper::billetera);
    }

    @GetMapping("/{codigo}/saldo")
    public BigDecimal saldo(@PathVariable String codigo) {
        return billeteras.consultarSaldo(codigo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.BilleteraResponse crear(@Valid @RequestBody ApiDtos.BilleteraRequest request) {
        return ApiMapper.billetera(billeteras.crearBilletera(request.idUsuario(), request.codigo(),
                request.nombre(), request.tipo()));
    }

    @PatchMapping("/{codigo}/estado")
    public ApiDtos.BilleteraResponse cambiarEstado(@PathVariable String codigo,
            @Valid @RequestBody ApiDtos.EstadoBilleteraRequest request) {
        return ApiMapper.billetera(billeteras.cambiarEstado(codigo, request.estado()));
    }

    @DeleteMapping("/{codigo}")
    public ApiDtos.BilleteraResponse eliminar(@PathVariable String codigo) {
        return ApiMapper.billetera(billeteras.eliminarBilletera(codigo));
    }
}
