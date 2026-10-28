package com.osinergmin.osinergmin.dto;

import java.time.Instant;

/** Reporte guardado en Alfresco. descripcion incluye los filtros y quién lo archivó. */
public record ReporteArchivadoResponse(
        String id, String nombre, String titulo, String descripcion, Instant fecha, Long tamanio) {
}
