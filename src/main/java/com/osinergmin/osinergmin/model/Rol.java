package com.osinergmin.osinergmin.model;

/**
 * Roles del sistema. Se guardan como texto en LAB_USUARIO.ROL y deben coincidir con la
 * restricción CK_LAB_USUARIO_ROL. El COORDINADOR administra los usuarios.
 */
public enum Rol {
    ANALISTA,
    COORDINADOR,
    APROBADOR
}
