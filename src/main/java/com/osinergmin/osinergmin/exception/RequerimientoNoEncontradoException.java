package com.osinergmin.osinergmin.exception;

import com.osinergmin.osinergmin.util.CodigoRequerimiento;

/** El requerimiento no existe → 404. */
public class RequerimientoNoEncontradoException extends RuntimeException {

    public RequerimientoNoEncontradoException(long id) {
        super("No existe el requerimiento " + CodigoRequerimiento.formatear(id));
    }
}
