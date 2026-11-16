package com.fintech.backend.dto;

import com.fintech.backend.enums.EstadoCuenta;
import com.fintech.backend.enums.TipoBilletera;
import com.fintech.backend.enums.TipoOperacionProgramada;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/** DTOs HTTP inmutables; evitan exponer referencias circulares del modelo. */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record UsuarioRequest(@NotBlank String nombre, @NotBlank @Email String correo) { }
    public record UsuarioUpdateRequest(@NotBlank String nombre, @NotBlank @Email String correo,
                                      @PositiveOrZero int puntos) { }
    public record UsuarioResponse(Long id, String nombre, String correo, BigDecimal saldo,
                                  int puntos, String nivel, String estado) { }

    public record BilleteraRequest(@Positive Long idUsuario, @NotBlank String codigo,
                                   @NotBlank String nombre, @NotNull TipoBilletera tipo) { }
    public record EstadoBilleteraRequest(@NotNull EstadoCuenta estado) { }
    public record BilleteraResponse(Long id, Long idUsuario, String codigo, String nombre,
                                    BigDecimal saldo, String estado, TipoBilletera tipo) { }

    public record RecargaRequest(@NotBlank String codigoDestino, @NotNull @Positive BigDecimal monto) { }
    public record RetiroRequest(@NotBlank String codigoOrigen, @NotNull @Positive BigDecimal monto) { }
    public record TransferenciaRequest(@NotBlank String codigoOrigen, @NotBlank String codigoDestino,
                                       @NotNull @Positive BigDecimal monto) { }
    public record TransaccionResponse(Long id, String tipo, BigDecimal valor, LocalDateTime fecha,
                                      String estado, Long idOrigen, Long idDestino,
                                      int puntosGenerados, boolean duplicada, String nivelRiesgo) { }

    public record ProgramadaRequest(@NotNull TipoOperacionProgramada tipo,
                                    String codigoOrigen, @NotBlank String codigoDestino,
                                    @NotNull @Positive BigDecimal monto,
                                    @NotNull LocalDateTime horaProgramada,
                                    @NotNull Duration periodicidad) { }
    public record SimulacionRequest(@NotNull Duration avanzar) { }
    public record OperacionProgramadaResponse(Long id, String tipo, LocalDateTime horaProgramada,
                                               String estado, String codigoOrigen, String codigoDestino,
                                               BigDecimal monto, Duration periodicidad) { }

    public record CanjeRequest(@Positive Long idBeneficio) { }
    public record BeneficioRequest(@NotBlank String nombre, @NotBlank String descripcion,
                                   @Positive int costoPuntos) { }
    public record BeneficioResponse(Long id, String nombre, String descripcion, int costoPuntos) { }
    public record CanjeResponse(Long id, Long idUsuario, Long idBeneficio, String beneficio,
                                int puntosDescontados, LocalDateTime fecha) { }
    public record ReversionRequest(@NotNull @jakarta.validation.constraints.Positive Long idTransaccion,
                                   @NotBlank String motivo) { }
    public record ReversionResponse(Long id, Long idTransaccion, Long idUsuario,
                                    LocalDateTime fecha, String motivo) { }

    public record NotificacionResponse(Long id, String tipo, String mensaje,
                                       LocalDateTime fecha, Long idUsuario) { }
    public record SimulacionInformeRequest(@NotNull LocalDateTime desde,
                                           @NotNull LocalDateTime hasta,
                                           @PositiveOrZero int k) { }
    public record RangoRequest(@NotNull LocalDateTime desde, @NotNull LocalDateTime hasta) { }

    public record AristaResponse(Long origen, Long destino, BigDecimal montoTotal, int veces) { }
    public record ConteoResponse(String elemento, int cantidad) { }
    public record ErrorResponse(String timestamp, int status, String error, String message,
                                java.util.Map<String, String> fieldErrors) { }
}
