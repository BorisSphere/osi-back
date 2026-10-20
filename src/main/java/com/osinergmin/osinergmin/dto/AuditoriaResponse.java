package com.osinergmin.osinergmin.dto;

import com.osinergmin.osinergmin.model.Auditoria;
import com.osinergmin.osinergmin.util.CodigoRequerimiento;
import java.time.Instant;

/** Evento de auditoría tal como lo muestra la pantalla de historial. */
public record AuditoriaResponse(
        long id, Instant fecha, String usuario, String operacion, String requerimientoCodigo, String detalle) {

    public static AuditoriaResponse from(Auditoria a) {
        return new AuditoriaResponse(
                a.getId(),
                a.getFecha(),
                a.getActor().getUsername(),
                a.getAccion().name(),
                CodigoRequerimiento.formatear(a.getRequerimientoId()),
                a.getDetalle());
    }
}
