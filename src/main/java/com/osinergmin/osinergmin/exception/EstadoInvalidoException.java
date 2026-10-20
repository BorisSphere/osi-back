package com.osinergmin.osinergmin.exception;

/** La operación no se permite en el estado actual del requerimiento → 409. */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String message) {
        super(message);
    }
}
