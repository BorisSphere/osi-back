package com.osinergmin.osinergmin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Respuesta de GET /api/reportes/seguimiento. porEstado siempre trae los cuatro estados. */
public record SeguimientoResponse(
        long total,
        Map<String, Long> porEstado,
        BigDecimal hhAprobadas,
        List<FilaAplicacion> porAplicacion) {

    /** Fila del resumen por aplicación (vista para coordinación). */
    public record FilaAplicacion(String aplicacion, Map<String, Long> porEstado, BigDecimal hhAprobadas) {}
}
