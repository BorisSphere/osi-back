package com.osinergmin.osinergmin.exception;

/** Token válido, pero el usuario no puede ingresar (sin rol asignado, no registrado, deshabilitado). */
public class EntraAccesoDenegadoException extends RuntimeException {

    public EntraAccesoDenegadoException(String message) {
        super(message);
    }
}
