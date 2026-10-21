package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.dto.AuditoriaResponse;
import com.osinergmin.osinergmin.service.AuditoriaService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/** Consulta de auditoría (RF07). Según la matriz de permisos, los tres roles pueden consultarla. */
@RestController
@PreAuthorize("hasAuthority('AUDITORIA_VER')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    /** Historial de un requerimiento. */
    @GetMapping("/api/requerimientos/{id}/auditoria")
    public List<AuditoriaResponse> historial(@PathVariable long id) {
        return auditoriaService.historialDeRequerimiento(id);
    }

    /** Historial global con filtros opcionales: ?desde=2026-10-01&hasta=2026-10-31&usuario=aprobador1 */
    @GetMapping("/api/auditoria")
    public List<AuditoriaResponse> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuario) {
        return auditoriaService.buscar(desde, hasta, usuario);
    }
}
