package com.osinergmin.osinergmin.service;

/** Snapshot inmutable del requerimiento: no transporta entidades JPA al proceso asíncrono. */
public record CorreoRequerimientoEvent(
        Tipo tipo,
        String destinatario,
        String codigo,
        String aplicacion,
        String titulo,
        String descripcion,
        String prioridad,
        String fechaRequerida,
        String detalleTecnico,
        String estado,
        String creador,
        String actor,
        String detalleAccion,
        String enlace) {

    public enum Tipo {
        REGISTRADO,
        ESTIMADO,
        APROBADO,
        RECHAZADO
    }
}
