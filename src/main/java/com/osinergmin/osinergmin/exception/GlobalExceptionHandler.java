package com.osinergmin.osinergmin.exception;

import com.osinergmin.osinergmin.dto.ErrorResponse;
import com.osinergmin.osinergmin.security.RefreshCookieService;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final RefreshCookieService cookieService;

    public GlobalExceptionHandler(RefreshCookieService cookieService) {
        this.cookieService = cookieService;
    }

    /** Refresh fallido: 401 y se borra la cookie para que el front vuelva al login. */
    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleRefreshInvalido(RefreshTokenInvalidoException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieService.limpiar().toString())
                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Datos inválidos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido (revise el JSON y el rol)", null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos", null);
    }

    @ExceptionHandler(EntraTokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleEntraTokenInvalido(EntraTokenInvalidoException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(EntraAccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> handleEntraAccesoDenegado(EntraAccesoDenegadoException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        return build(HttpStatus.FORBIDDEN, "El usuario está inactivo", null);
    }

    @ExceptionHandler(UsuarioYaExisteException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(UsuarioYaExisteException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(AplicacionYaExisteException.class)
    public ResponseEntity<ErrorResponse> handleAplicacionDuplicada(AplicacionYaExisteException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(AplicacionNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleAplicacionNoEncontrada(AplicacionNoEncontradaException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(RequerimientoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRequerimientoNoEncontrado(RequerimientoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(RequerimientoNoEditableException.class)
    public ResponseEntity<ErrorResponse> handleRequerimientoNoEditable(RequerimientoNoEditableException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(ArchivoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleArchivoNoEncontrado(ArchivoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleArchivoInvalido(ArchivoInvalidoException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleArchivoGrande(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido", null);
    }

    /** Parámetro con valor inválido, p. ej. ?estado=XYZ. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro '" + ex.getName() + "'", null);
    }

    /** Restricción de la BD (UNIQUE, FK): p. ej. eliminar una aplicación que ya tiene requerimientos. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Restricción de integridad violada: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "La operación no se puede completar porque el registro está en uso o duplicado", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Map<String, String> campos) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message, campos));
    }

    /** @PreAuthorize denegado dentro de un controlador. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso", null);
    }

    /** Cualquier error no previsto: se registra en el log y al cliente solo le llega un mensaje genérico. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Errores propios de Spring MVC (404, 405, 415...) conservan su código HTTP.
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return build(status, status.getReasonPhrase(), null);
        }
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Intente nuevamente.", null);
    }
}
