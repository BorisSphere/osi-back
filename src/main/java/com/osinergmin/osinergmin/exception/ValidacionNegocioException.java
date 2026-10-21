package com.osinergmin.osinergmin.exception;

/**
 * Dato inválido según una regla de negocio que depende de otros datos (p. ej. el motivo solo es
 * obligatorio al rechazar) → 400, con el campo afectado en "campos" como en las validaciones normales.
 */
public class ValidacionNegocioException extends RuntimeException {

    private final String campo;

    public ValidacionNegocioException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
