package com.osinergmin.osinergmin.exception;

import com.osinergmin.osinergmin.dto.ErrorResponse;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errores de los flujos de estimación y aprobación. Va antes que GlobalExceptionHandler porque
 * aquel convierte cualquier excepción no prevista en 500; este solo atiende las suyas y el resto
 * sigue llegando al manejador global.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FlujoRequerimientoExceptionHandler {

    @ExceptionHandler(RequerimientoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RequerimientoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleEstadoInvalido(EstadoInvalidoException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(ValidacionNegocioException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ErrorResponse.of(
                status.value(), status.getReasonPhrase(), "Datos inválidos", Map.of(ex.getCampo(), ex.getMessage())));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message));
    }
}
