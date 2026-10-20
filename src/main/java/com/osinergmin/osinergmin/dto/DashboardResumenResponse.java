package com.osinergmin.osinergmin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Respuesta de GET /api/dashboard/resumen. porEstado siempre trae los cuatro estados. */
public record DashboardResumenResponse(
        long aplicaciones,
        long requerimientos,
        Map<String, Long> porEstado,
        BigDecimal hhAprobadas,
        List<PorAplicacion> porAplicacion) {

    public record PorAplicacion(String aplicacion, long cantidad) {}
}
