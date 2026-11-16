package com.fintech.backend.config;

import com.fintech.backend.service.BilleteraService;
import com.fintech.backend.service.AnaliticaService;
import com.fintech.backend.service.NotificacionService;
import com.fintech.backend.service.OperacionProgramadaService;
import com.fintech.backend.service.RecompensasService;
import com.fintech.backend.service.SimulacionService;
import com.fintech.backend.service.TransaccionService;
import com.fintech.backend.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla los servicios de dominio en memoria usados por la API REST. */
@Configuration
public class ApiServicesConfig {

    @Bean
    public UsuarioService usuarioService() {
        return new UsuarioService();
    }

    @Bean
    public BilleteraService billeteraService(UsuarioService usuarioService) {
        return new BilleteraService(usuarioService);
    }

    @Bean
    public TransaccionService transaccionService(UsuarioService usuarioService,
                                                  BilleteraService billeteraService,
                                                  com.fintech.backend.service.Reloj reloj) {
        return new TransaccionService(usuarioService, billeteraService, reloj);
    }

    @Bean
    public RecompensasService recompensasService(TransaccionService transaccionService) {
        return transaccionService.getRecompensasService();
    }

    @Bean
    public NotificacionService notificacionService(TransaccionService transaccionService) {
        return transaccionService.getNotificacionService();
    }

    @Bean
    public AnaliticaService analiticaService(TransaccionService transaccionService) {
        return transaccionService.getAnaliticaService();
    }

    @Bean
    public OperacionProgramadaService operacionProgramadaService(
            TransaccionService transaccionService, com.fintech.backend.service.Reloj reloj) {
        return new OperacionProgramadaService(transaccionService, reloj);
    }

    @Bean
    public SimulacionService simulacionService(
            com.fintech.backend.service.Reloj reloj,
            OperacionProgramadaService operacionProgramadaService) {
        return new SimulacionService(reloj, operacionProgramadaService);
    }
}
