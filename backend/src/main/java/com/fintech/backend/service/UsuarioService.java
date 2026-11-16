package com.fintech.backend.service;

import com.fintech.backend.model.Recompensas;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.structures.hash.TablaHash;
import com.fintech.backend.structures.list.ListaEnlazada;
import com.fintech.backend.structures.tree.ArbolAVL;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Administra usuarios en memoria con índices por id/correo y por puntos.
 *
 * <p>Un usuario solo se elimina cuando no tiene billeteras asociadas. Así se
 * preserva la integridad del índice de billeteras; primero deben eliminarse
 * las billeteras sin saldo. Las billeteras con saldo nunca se pueden eliminar.</p>
 */
public class UsuarioService {

    private final TablaHash<Long, Usuario> usuariosPorId = new TablaHash<>();
    private final TablaHash<String, Usuario> usuariosPorCorreo = new TablaHash<>();
    private final TablaHash<Long, ClavePuntos> clavesPuntosPorId = new TablaHash<>();
    private final ArbolAVL<ClavePuntos, Usuario> usuariosPorPuntos = new ArbolAVL<>();

    /**
     * Registra un usuario y actualiza sus índices.
     *
     * @throws IllegalArgumentException si ya existe el id o el correo
     * @throws NullPointerException si el usuario es null
     */
    public Usuario registrarUsuario(Usuario usuario) {
        Objects.requireNonNull(usuario, "El usuario no puede ser null");
        validarDatos(usuario.getNombre(), usuario.getCorreo(), usuario.getPuntosAcumulados());
        String correo = normalizarCorreo(usuario.getCorreo());
        if (usuariosPorId.contieneClave(usuario.getIdUsuario())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese id");
        }
        if (usuariosPorCorreo.contieneClave(correo)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo");
        }
        usuario.setNivel(new Recompensas(usuario).calcularNivel(usuario.getPuntosAcumulados()));
        usuariosPorId.poner(usuario.getIdUsuario(), usuario);
        usuariosPorCorreo.poner(correo, usuario);
        ClavePuntos clavePuntos = clave(usuario);
        usuariosPorPuntos.insertar(clavePuntos, usuario);
        clavesPuntosPorId.poner(usuario.getIdUsuario(), clavePuntos);
        return usuario;
    }

    /** Crea y registra un usuario nuevo. */
    public Usuario registrarUsuario(String nombre, String correo) {
        Usuario usuario = new Usuario(nombre, correo);
        return registrarUsuario(usuario);
    }

    /**
     * Modifica nombre, correo y puntos, manteniendo consistentes los índices.
     *
     * @throws NoSuchElementException si el id no está registrado
     * @throws IllegalArgumentException si el correo pertenece a otro usuario
     */
    public Usuario modificarUsuario(Long id, String nombre, String correo, int puntosAcumulados) {
        Objects.requireNonNull(id, "El id no puede ser null");
        validarDatos(nombre, correo, puntosAcumulados);
        Usuario usuario = buscarUsuario(id);
        String nuevoCorreo = normalizarCorreo(correo);
        Usuario propietarioCorreo = usuariosPorCorreo.obtener(nuevoCorreo);
        if (propietarioCorreo != null && !propietarioCorreo.getIdUsuario().equals(id)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo");
        }

        usuariosPorPuntos.eliminar(clavesPuntosPorId.obtener(id));
        usuariosPorCorreo.eliminar(normalizarCorreo(usuario.getCorreo()));
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setPuntosAcumulados(puntosAcumulados);
        usuario.setNivel(new Recompensas(usuario).calcularNivel(puntosAcumulados));
        usuariosPorCorreo.poner(nuevoCorreo, usuario);
        ClavePuntos nuevaClavePuntos = clave(usuario);
        usuariosPorPuntos.insertar(nuevaClavePuntos, usuario);
        clavesPuntosPorId.poner(id, nuevaClavePuntos);
        return usuario;
    }

    /**
     * Cambia puntos por una operación (delta positivo) o un canje (delta
     * negativo), actualizando en conjunto el índice AVL y el nivel.
     */
    public Usuario ajustarPuntos(Long id, int delta) {
        Usuario usuario = buscarUsuario(id);
        int nuevosPuntos;
        try {
            nuevosPuntos = Math.addExact(usuario.getPuntosAcumulados(), delta);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("El total de puntos excede el rango permitido", exception);
        }
        if (nuevosPuntos < 0) {
            throw new IllegalArgumentException("El saldo de puntos no puede ser negativo");
        }
        ClavePuntos claveAnterior = clavesPuntosPorId.obtener(id);
        usuariosPorPuntos.eliminar(claveAnterior);
        usuario.setPuntosAcumulados(nuevosPuntos);
        usuario.setNivel(new Recompensas(usuario).calcularNivel(nuevosPuntos));
        ClavePuntos nuevaClave = clave(usuario);
        usuariosPorPuntos.insertar(nuevaClave, usuario);
        clavesPuntosPorId.poner(id, nuevaClave);
        return usuario;
    }

    /** Busca por id; lanza excepción si no existe. */
    public Usuario buscarUsuario(Long id) {
        Objects.requireNonNull(id, "El id no puede ser null");
        Usuario usuario = usuariosPorId.obtener(id);
        if (usuario == null) {
            throw new NoSuchElementException("No existe el usuario con id " + id);
        }
        return usuario;
    }

    /** Busca por correo sin distinguir mayúsculas/minúsculas; null si no existe. */
    public Usuario buscarPorCorreo(String correo) {
        Objects.requireNonNull(correo, "El correo no puede ser null");
        return usuariosPorCorreo.obtener(normalizarCorreo(correo));
    }

    /**
     * Elimina usuarios únicamente cuando no conservan billeteras asociadas,
     * incluso si estas tienen saldo cero. Las billeteras deben eliminarse
     * primero a través de {@link BilleteraService#eliminarBilletera(String)}.
     */
    public Usuario eliminarUsuario(Long id) {
        Usuario usuario = buscarUsuario(id);
        if (!usuario.getBilleteras().estaVacia()) {
            throw new IllegalStateException(
                    "No se puede eliminar un usuario con billeteras; elimine primero las billeteras sin saldo");
        }
        usuariosPorId.eliminar(id);
        usuariosPorCorreo.eliminar(normalizarCorreo(usuario.getCorreo()));
        usuariosPorPuntos.eliminar(clavesPuntosPorId.eliminar(id));
        return usuario;
    }

    /** Devuelve usuarios dentro del rango inclusivo de puntos, ordenados por puntos e id. */
    public ListaEnlazada<Usuario> buscarPorRangoDePuntos(int minimo, int maximo) {
        if (minimo < 0 || maximo < 0) {
            throw new IllegalArgumentException("Los puntos no pueden ser negativos");
        }
        return usuariosPorPuntos.rango(
                new ClavePuntos(minimo, Long.MIN_VALUE),
                new ClavePuntos(maximo, Long.MAX_VALUE));
    }

    /** Devuelve todos los usuarios ordenados por puntos e id. */
    public ListaEnlazada<Usuario> listarPorPuntos() {
        return usuariosPorPuntos.recorridoInOrden();
    }

    /** Devuelve el ranking de mayor a menor puntuación. */
    public ListaEnlazada<Usuario> listarPorPuntosDescendente() {
        return usuariosPorPuntos.recorridoDescendente();
    }

    public int cantidadUsuarios() {
        return usuariosPorId.size();
    }

    private static void validarDatos(String nombre, String correo, int puntos) {
        Objects.requireNonNull(nombre, "El nombre no puede ser null");
        Objects.requireNonNull(correo, "El correo no puede ser null");
        if (nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (puntos < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos");
        if (!correo.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El correo no tiene un formato válido");
        }
    }

    private static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private static ClavePuntos clave(Usuario usuario) {
        return new ClavePuntos(usuario.getPuntosAcumulados(), usuario.getIdUsuario());
    }

    private static final class ClavePuntos implements Comparable<ClavePuntos> {
        private final int puntos;
        private final long id;

        private ClavePuntos(int puntos, long id) {
            this.puntos = puntos;
            this.id = id;
        }

        @Override
        public int compareTo(ClavePuntos otra) {
            int porPuntos = Integer.compare(puntos, otra.puntos);
            return porPuntos != 0 ? porPuntos : Long.compare(id, otra.id);
        }
    }
}
