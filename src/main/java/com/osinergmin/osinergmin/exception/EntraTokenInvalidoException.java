package com.osinergmin.osinergmin.exception;

/** El token de Microsoft Entra ID no es válido (firma, emisor, audiencia, vencimiento o scope). */
public class EntraTokenInvalidoException extends RuntimeException {

    public EntraTokenInvalidoException(String message) {
        super(message);
    }
}
