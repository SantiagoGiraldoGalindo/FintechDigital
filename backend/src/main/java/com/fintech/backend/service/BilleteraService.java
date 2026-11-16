package com.fintech.backend.service;

import com.fintech.backend.enums.EstadoCuenta;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.structures.hash.TablaHash;
import com.fintech.backend.structures.list.ListaEnlazada;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Administra billeteras y su índice único por código. */
public class BilleteraService {

    private final UsuarioService usuarioService;
    private final TablaHash<String, BilleteraDigital> billeterasPorCodigo = new TablaHash<>();

    public BilleteraService(UsuarioService usuarioService) {
        this.usuarioService = Objects.requireNonNull(usuarioService, "El servicio de usuarios no puede ser null");
    }

    /**
     * Crea una billetera para un usuario registrado. Un usuario puede tener
     * varias billeteras, pero cada código debe ser único.
     */
    public BilleteraDigital crearBilletera(Long idUsuario, String codigo,
                                            String nombre, TipoBilletera tipo) {
        Objects.requireNonNull(idUsuario, "El id de usuario no puede ser null");
        String codigoNormalizado = normalizarCodigo(codigo);
        validarNombre(nombre);
        Objects.requireNonNull(tipo, "El tipo de billetera no puede ser null");
        if (billeterasPorCodigo.contieneClave(codigoNormalizado)) {
            throw new IllegalArgumentException("Ya existe una billetera con ese código");
        }
        Usuario usuario = usuarioService.buscarUsuario(idUsuario);
        BilleteraDigital billetera = new BilleteraDigital(codigoNormalizado, nombre, tipo, usuario);
        billeterasPorCodigo.poner(codigoNormalizado, billetera);
        usuario.getBilleteras().agregar(billetera);
        actualizarSaldoUsuario(usuario);
        return billetera;
    }

    /** Busca una billetera por código, sin distinguir mayúsculas/minúsculas. */
    public BilleteraDigital buscarBilletera(String codigo) {
        String codigoNormalizado = normalizarCodigo(codigo);
        BilleteraDigital billetera = billeterasPorCodigo.obtener(codigoNormalizado);
        if (billetera == null) {
            throw new NoSuchElementException("No existe la billetera con código " + codigo);
        }
        return billetera;
    }

    /** Cambia el código de la billetera, rechazando duplicados. */
    public BilleteraDigital cambiarCodigo(String codigoActual, String nuevoCodigo) {
        String actualNormalizado = normalizarCodigo(codigoActual);
        String nuevoNormalizado = normalizarCodigo(nuevoCodigo);
        BilleteraDigital billetera = buscarBilletera(actualNormalizado);
        if (!actualNormalizado.equals(nuevoNormalizado)
                && billeterasPorCodigo.contieneClave(nuevoNormalizado)) {
            throw new IllegalArgumentException("Ya existe una billetera con ese código");
        }
        billeterasPorCodigo.eliminar(actualNormalizado);
        billetera.setCodigo(nuevoNormalizado);
        billeterasPorCodigo.poner(nuevoNormalizado, billetera);
        return billetera;
    }

    /** Modifica el nombre o tipo de billetera. */
    public BilleteraDigital modificarBilletera(String codigo, String nombre, TipoBilletera tipo) {
        validarNombre(nombre);
        Objects.requireNonNull(tipo, "El tipo de billetera no puede ser null");
        BilleteraDigital billetera = buscarBilletera(codigo);
        billetera.setNombre(nombre);
        billetera.setTipoBilletera(tipo);
        return billetera;
    }

    /** Cambia el estado de la billetera. */
    public BilleteraDigital cambiarEstado(String codigo, EstadoCuenta estado) {
        Objects.requireNonNull(estado, "El estado no puede ser null");
        BilleteraDigital billetera = buscarBilletera(codigo);
        billetera.setEstadoCuenta(estado);
        return billetera;
    }

    /** Devuelve el saldo disponible actual. */
    public BigDecimal consultarSaldo(String codigo) {
        return buscarBilletera(codigo).getSaldoDisponible();
    }

    /**
     * Actualiza el saldo disponible y recalcula el saldo agregado del usuario.
     * El saldo no puede ser negativo.
     */
    public BigDecimal actualizarSaldo(String codigo, BigDecimal saldo) {
        Objects.requireNonNull(saldo, "El saldo no puede ser null");
        if (saldo.signum() < 0) {
            throw new IllegalArgumentException("El saldo no puede ser negativo");
        }
        BilleteraDigital billetera = buscarBilletera(codigo);
        billetera.setSaldoDisponible(saldo);
        actualizarSaldoUsuario(billetera.getUsuario());
        return saldo;
    }

    /** Devuelve las billeteras del usuario en orden de creación. */
    public ListaEnlazada<BilleteraDigital> billeterasDeUsuario(Long idUsuario) {
        return usuarioService.buscarUsuario(idUsuario).getBilleteras();
    }

    /**
     * Elimina una billetera únicamente si su saldo es cero. Al eliminarla se
     * retira del índice y de la colección del usuario.
     */
    public BilleteraDigital eliminarBilletera(String codigo) {
        BilleteraDigital billetera = buscarBilletera(codigo);
        if (billetera.getSaldoDisponible().signum() != 0) {
            throw new IllegalStateException("No se puede eliminar una billetera con saldo");
        }
        billeterasPorCodigo.eliminar(normalizarCodigo(codigo));
        Usuario usuario = billetera.getUsuario();
        usuario.getBilleteras().eliminar(billetera);
        actualizarSaldoUsuario(usuario);
        return billetera;
    }

    public int cantidadBilleteras() {
        return billeterasPorCodigo.size();
    }

    private void actualizarSaldoUsuario(Usuario usuario) {
        BigDecimal saldoTotal = BigDecimal.ZERO;
        for (BilleteraDigital billetera : usuario.getBilleteras()) {
            saldoTotal = saldoTotal.add(billetera.getSaldoDisponible());
        }
        usuario.setSaldo(saldoTotal);
    }

    private static String normalizarCodigo(String codigo) {
        Objects.requireNonNull(codigo, "El código no puede ser null");
        String normalizado = codigo.trim().toUpperCase(Locale.ROOT);
        if (normalizado.isEmpty()) throw new IllegalArgumentException("El código es obligatorio");
        return normalizado;
    }

    private static void validarNombre(String nombre) {
        Objects.requireNonNull(nombre, "El nombre no puede ser null");
        if (nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
    }
}
