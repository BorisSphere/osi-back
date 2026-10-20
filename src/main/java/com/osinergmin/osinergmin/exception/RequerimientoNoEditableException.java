package com.osinergmin.osinergmin.exception;

public class RequerimientoNoEditableException extends RuntimeException {

    public RequerimientoNoEditableException(Long id) {
        super("El requerimiento " + id + " ya está aprobado y no se puede modificar");
    }
}
