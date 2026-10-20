package com.osinergmin.osinergmin.exception;

public class RequerimientoNoEncontradoException extends RuntimeException {

    public RequerimientoNoEncontradoException(Long id) {
        super("No se encontró el requerimiento con id " + id);
    }
}
