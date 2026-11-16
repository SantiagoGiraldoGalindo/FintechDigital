package com.fintech.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fintech.backend.enums.NivelUsuario;
import com.fintech.backend.model.Usuario;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class UsuarioServiceTest {

    @Test
    void registraBuscaModificaYActualizaIndices() {
        UsuarioService service = new UsuarioService();
        Usuario ana = new Usuario(" Ana ", "ANA@example.com");
        Usuario bob = new Usuario("Bob", "bob@example.com");
        ana.setPuntosAcumulados(100);
        bob.setPuntosAcumulados(100);
        service.registrarUsuario(ana);
        service.registrarUsuario(bob);

        assertEquals(ana, service.buscarUsuario(ana.getIdUsuario()));
        assertEquals(ana, service.buscarPorCorreo("ana@EXAMPLE.com"));
        assertEquals(2, service.buscarPorRangoDePuntos(100, 100).size());

        service.modificarUsuario(ana.getIdUsuario(), "Ana María", "ana.maria@example.com", 1_500);
        assertEquals("Ana María", service.buscarUsuario(ana.getIdUsuario()).getNombre());
        assertEquals(ana, service.buscarPorCorreo("ana.maria@example.com"));
        assertEquals(NivelUsuario.ORO, ana.getNivel());
        assertEquals(1, service.buscarPorRangoDePuntos(1_500, 1_500).size());
        assertEquals(1, service.buscarPorRangoDePuntos(100, 100).size());
        assertEquals(2, service.cantidadUsuarios());
    }

    @Test
    void rechazaDatosDuplicadosInvalidosYUsuariosInexistentes() {
        UsuarioService service = new UsuarioService();
        Usuario registrado = service.registrarUsuario("Ana", "ana@example.com");
        service.registrarUsuario("Bob", "bob@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> service.registrarUsuario("Otra", "ANA@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarUsuario(" ", "otro@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarUsuario("Otra", "correo-invalido"));
        assertThrows(IllegalArgumentException.class,
                () -> service.modificarUsuario(registrado.getIdUsuario(), "Ana", "otro@example.com", -1));
        assertThrows(IllegalArgumentException.class,
                () -> service.modificarUsuario(registrado.getIdUsuario(), "Ana", "bob@example.com", 0));
        assertThrows(NoSuchElementException.class, () -> service.buscarUsuario(-1L));
        assertThrows(NoSuchElementException.class, () -> service.eliminarUsuario(-1L));
        assertThrows(IllegalArgumentException.class,
                () -> service.buscarPorRangoDePuntos(-1, 5));

        assertEquals(registrado, service.eliminarUsuario(registrado.getIdUsuario()));
        Usuario bob = service.buscarPorCorreo("bob@example.com");
        service.eliminarUsuario(bob.getIdUsuario());
        assertTrue(service.listarPorPuntos().estaVacia());
        assertFalse(service.cantidadUsuarios() > 0);
    }

    @Test
    void noPermiteEliminarUsuarioConBilleteras() {
        UsuarioService usuarios = new UsuarioService();
        BilleteraService billeteras = new BilleteraService(usuarios);
        Usuario usuario = usuarios.registrarUsuario("Ana", "ana@example.com");
        billeteras.crearBilletera(usuario.getIdUsuario(), "A-1", "Ahorros",
                com.fintech.backend.enums.TipoBilletera.AHORRO);

        assertThrows(IllegalStateException.class, () -> usuarios.eliminarUsuario(usuario.getIdUsuario()));
        assertEquals(usuario, usuarios.buscarUsuario(usuario.getIdUsuario()));
    }
}
