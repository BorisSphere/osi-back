package com.osinergmin.osinergmin.model;

/**
 * Valores de LAB_AUDITORIA.ACCION (la columna no tiene CHECK; estos son los acordados).
 * REGISTRO y ACTUALIZACION los escribe el módulo de requerimientos; el resto, estimación y aprobación.
 */
public enum AccionAuditoria {
    REGISTRO,
    ACTUALIZACION,
    ESTIMACION,
    APROBACION,
    RECHAZO
}
