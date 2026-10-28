package com.osinergmin.osinergmin.alfresco;

/** Alfresco no está configurado, no responde o rechazó las credenciales → 503. */
public class AlfrescoNoDisponibleException extends RuntimeException {

    public AlfrescoNoDisponibleException(String message) {
        super(message);
    }

    public AlfrescoNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
