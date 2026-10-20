package com.osinergmin.osinergmin.exception;

public class AplicacionYaExisteException extends RuntimeException {

    public AplicacionYaExisteException(String nombre) {
        super("Ya existe una aplicación con el nombre '" + nombre + "'");
    }
}
