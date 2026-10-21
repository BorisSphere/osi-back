package com.osinergmin.osinergmin.dto;

import com.osinergmin.osinergmin.model.Estimacion;
import com.osinergmin.osinergmin.util.CodigoRequerimiento;
import java.math.BigDecimal;

public record EstimacionResponse(
        long requerimientoId,
        String codigo,
        String estado,
        BigDecimal hhAnalisis,
        BigDecimal hhDesarrollo,
        BigDecimal hhPruebas,
        BigDecimal hhDocumentacion,
        BigDecimal hhTotal) {

    public static EstimacionResponse from(Estimacion e, String estado) {
        return new EstimacionResponse(
                e.getRequerimientoId(),
                CodigoRequerimiento.formatear(e.getRequerimientoId()),
                estado,
                e.getHhAnalisis(),
                e.getHhDesarrollo(),
                e.getHhPruebas(),
                e.getHhDocumentacion(),
                e.getHhTotal());
    }
}
