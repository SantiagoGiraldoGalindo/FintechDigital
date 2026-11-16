package com.fintech.backend.config;

import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.model.BilleteraDigital;
import com.fintech.backend.model.Usuario;
import com.fintech.backend.service.BilleteraService;
import com.fintech.backend.service.Reloj;
import com.fintech.backend.service.TransaccionService;
import com.fintech.backend.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Datos navegables para desarrollo local; se omiten en perfiles explícitos. */
@Component
@Profile("default")
public class DatosSemilla implements ApplicationRunner {

    private final UsuarioService usuarios;
    private final BilleteraService billeteras;
    private final TransaccionService transacciones;
    private final Reloj reloj;

    public DatosSemilla(UsuarioService usuarios, BilleteraService billeteras,
                        TransaccionService transacciones, Reloj reloj) {
        this.usuarios = usuarios;
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.cantidadUsuarios() > 0) return;
        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        reloj.fijar(base);
        Usuario ana = crearUsuario("Ana Torres", "ana@demo.local", 120);
        Usuario bruno = crearUsuario("Bruno Díaz", "bruno@demo.local", 720);
        Usuario carla = crearUsuario("Carla Ruiz", "carla@demo.local", 1_800);
        Usuario diego = crearUsuario("Diego León", "diego@demo.local", 5_200);
        Usuario elena = crearUsuario("Elena Paz", "elena@demo.local", 350);
        Usuario fabian = crearUsuario("Fabián Sol", "fabian@demo.local", 3_000);

        BilleteraDigital anaAhorro = wallet(ana, "DEMO-ANA-A", TipoBilletera.AHORRO);
        BilleteraDigital anaDiario = wallet(ana, "DEMO-ANA-D", TipoBilletera.GASTOS_DIARIOS);
        BilleteraDigital brunoWallet = wallet(bruno, "DEMO-BRUNO", TipoBilletera.COMPRAS);
        BilleteraDigital carlaWallet = wallet(carla, "DEMO-CARLA", TipoBilletera.INVERSION);
        BilleteraDigital diegoWallet = wallet(diego, "DEMO-DIEGO", TipoBilletera.SUSCRIPCIONES);
        BilleteraDigital elenaWallet = wallet(elena, "DEMO-ELENA", TipoBilletera.TRANSPORTE);
        BilleteraDigital fabianWallet = wallet(fabian, "DEMO-FABIAN", TipoBilletera.AHORRO);

        saldo("DEMO-ANA-A", "1500");
        saldo("DEMO-ANA-D", "500");
        saldo("DEMO-BRUNO", "800");
        saldo("DEMO-CARLA", "1200");
        saldo("DEMO-DIEGO", "2400");
        saldo("DEMO-ELENA", "300");
        saldo("DEMO-FABIAN", "900");

        transferir(anaAhorro, brunoWallet, "25", base);
        transferir(brunoWallet, carlaWallet, "18", base.plusMinutes(1));
        transferir(carlaWallet, anaAhorro, "12", base.plusMinutes(2));
        transferir(anaAhorro, brunoWallet, "25", base.plusMinutes(3));
        transferir(anaDiario, diegoWallet, "15", base.plusMinutes(4));
        transferir(diegoWallet, fabianWallet, "22", base.plusMinutes(5));
        transferir(fabianWallet, anaDiario, "9", base.plusMinutes(6));
        transferir(anaAhorro, elenaWallet, "10", base.plusMinutes(7));
        transferir(elenaWallet, anaAhorro, "5", base.plusMinutes(8));
        reloj.fijar(base.plusMinutes(8));
    }

    private Usuario crearUsuario(String nombre, String correo, int puntos) {
        Usuario usuario = new Usuario(nombre, correo);
        usuario.setPuntosAcumulados(puntos);
        return usuarios.registrarUsuario(usuario);
    }

    private BilleteraDigital wallet(Usuario usuario, String codigo, TipoBilletera tipo) {
        return billeteras.crearBilletera(usuario.getIdUsuario(), codigo, codigo, tipo);
    }

    private void saldo(String codigo, String valor) {
        billeteras.actualizarSaldo(codigo, new BigDecimal(valor));
    }

    private void transferir(BilleteraDigital origen, BilleteraDigital destino, String monto,
                            LocalDateTime fecha) {
        reloj.fijar(fecha);
        transacciones.realizarTransaccion(origen, destino, new BigDecimal(monto));
    }
}
