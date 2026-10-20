package com.osinergmin.osinergmin.dto;

import com.osinergmin.osinergmin.model.EstadoRequerimiento;
import com.osinergmin.osinergmin.model.Prioridad;
import com.osinergmin.osinergmin.model.Requerimiento;
import java.time.Instant;
import java.time.LocalDate;

public record RequerimientoResponse(
        Long id,
        String codigo,
        AplicacionResumen aplicacion,
        String titulo,
        String descripcion,
        Prioridad prioridad,
        EstadoRequerimiento estado,
        String tipo,
        LocalDate fechaRequerida,
        String detalleTecnico,
        String archivoNombre,
        String creadoPor,
        Instant fechaCreacion,
        Instant fechaActualizacion) {

    public record AplicacionResumen(Long id, String codigo, String nombre) {
    }

    public static RequerimientoResponse from(Requerimiento r) {
        return new RequerimientoResponse(
                r.getId(),
                codigo(r),
                new AplicacionResumen(r.getAplicacion().getId(), r.getAplicacion().getCodigo(),
                        r.getAplicacion().getNombre()),
                r.getTitulo(),
                r.getDescripcion(),
                r.getPrioridad(),
                r.getEstado(),
                r.getTipo(),
                r.getFechaRequerida(),
                r.getDetalleTecnico(),
                r.getArchivoNombre(),
                r.getCreadoPor().getUsername(),
                r.getFechaCreacion(),
                r.getFechaActualizacion());
    }

    /** Código derivado (REQ-aaaa-nnn): no se guarda en la tabla. */
    private static String codigo(Requerimiento r) {
        int anio = r.getFechaCreacion().atZone(java.time.ZoneOffset.UTC).getYear();
        return String.format("REQ-%d-%03d", anio, r.getId());
    }
}
