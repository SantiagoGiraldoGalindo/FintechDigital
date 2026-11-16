package com.fintech.backend.controller;

import com.fintech.backend.dto.ApiDtos;
import com.fintech.backend.service.OperacionRechazadaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Formato JSON uniforme para errores de API. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> validacion(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Validación fallida", campos);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiDtos.ErrorResponse> solicitudInvalida(Exception exception) {
        return respuesta(HttpStatus.BAD_REQUEST, mensaje(exception), Map.of());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> noExiste(Exception exception) {
        return respuesta(HttpStatus.NOT_FOUND, mensaje(exception), Map.of());
    }

    @ExceptionHandler({IllegalStateException.class, OperacionRechazadaException.class})
    public ResponseEntity<ApiDtos.ErrorResponse> conflicto(Exception exception) {
        return respuesta(HttpStatus.CONFLICT, mensaje(exception), Map.of());
    }

    private static ResponseEntity<ApiDtos.ErrorResponse> respuesta(
            HttpStatus status, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(status).body(new ApiDtos.ErrorResponse(
                Instant.now().toString(), status.value(), status.getReasonPhrase(), mensaje, campos));
    }

    private static String mensaje(Exception exception) {
        String mensaje = exception.getMessage();
        return mensaje == null || mensaje.isBlank() ? "Solicitud no válida" : mensaje;
    }
}
