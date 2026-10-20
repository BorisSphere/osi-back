package com.osinergmin.osinergmin.exception;

public class AplicacionNoEncontradaException extends RuntimeException {

    public AplicacionNoEncontradaException(Long id) {
        super("No se encontró la aplicación con id " + id);
    }
}
