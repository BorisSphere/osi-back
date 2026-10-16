package com.osinergmin.osinergmin.exception;

public class UsuarioYaExisteException extends RuntimeException {

    public UsuarioYaExisteException(String username) {
        super("El usuario '" + username + "' ya existe");
    }
}
