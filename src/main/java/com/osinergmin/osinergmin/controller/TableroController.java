package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.dto.DashboardResumenResponse;
import com.osinergmin.osinergmin.dto.RequerimientoBandejaResponse;
import com.osinergmin.osinergmin.dto.SeguimientoResponse;
import com.osinergmin.osinergmin.service.TableroService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultas para las pantallas de Analista 3. Según la matriz de permisos, los tres roles ven
 * el tablero; basta con estar autenticado (SecurityConfig).
 */
@RestController
public class TableroController {

    private final TableroService tableroService;

    public TableroController(TableroService tableroService) {
        this.tableroService = tableroService;
    }

    /** Bandeja de estimación y aprobación: ?estado=ESTIMADO&estado=APROBADO (sin estado = todos). */
    @GetMapping("/api/estimaciones")
    public List<RequerimientoBandejaResponse> bandeja(@RequestParam(required = false) List<String> estado) {
        return tableroService.bandeja(estado);
    }

    @GetMapping("/api/dashboard/resumen")
    public DashboardResumenResponse resumen() {
        return tableroService.resumen();
    }

    /** ?desde=2026-10-01&hasta=2026-10-31&aplicacion=LAB-001 (todos opcionales). */
    @GetMapping("/api/reportes/seguimiento")
    public SeguimientoResponse seguimiento(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String aplicacion) {
        return tableroService.seguimiento(desde, hasta, aplicacion);
    }
}
