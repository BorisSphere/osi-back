package com.osinergmin.osinergmin.exception;

public class ArchivoNoEncontradoException extends RuntimeException {

    public ArchivoNoEncontradoException(Long requerimientoId) {
        super("El requerimiento " + requerimientoId + " no tiene un archivo adjunto");
    }
}
