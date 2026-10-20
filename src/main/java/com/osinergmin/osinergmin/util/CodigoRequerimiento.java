package com.osinergmin.osinergmin.util;

/**
 * Código visible del requerimiento. LAB_REQUERIMIENTO no tiene columna CODIGO, así que se arma
 * a partir del ID. Formato provisional acordado: REQ-0001 (si cambia, se cambia solo aquí).
 */
public final class CodigoRequerimiento {

    private CodigoRequerimiento() {
    }

    public static String formatear(long id) {
        return String.format("REQ-%04d", id);
    }
}
