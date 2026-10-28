package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.dto.ReporteArchivadoResponse;
import com.osinergmin.osinergmin.service.ReporteArchivoService;
import com.osinergmin.osinergmin.service.ReporteExportService.Archivo;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Archivo de reportes en Alfresco (gestor documental). Igual que Reportes, disponible para los
 * tres roles; Alfresco registra cada documento y la descripción guarda quién lo archivó.
 */
@RestController
public class ReporteArchivoController {

    private final ReporteArchivoService archivoService;

    public ReporteArchivoController(ReporteArchivoService archivoService) {
        this.archivoService = archivoService;
    }

    /** Genera el PDF del seguimiento con los filtros y lo guarda en Alfresco. */
    @PostMapping("/api/reportes/seguimiento/archivar")
    public ResponseEntity<ReporteArchivadoResponse> archivar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String aplicacion,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(archivoService.archivar(desde, hasta, aplicacion, jwt.getSubject()));
    }

    @GetMapping("/api/reportes/archivados")
    public List<ReporteArchivadoResponse> listar() {
        return archivoService.listar();
    }

    @GetMapping("/api/reportes/archivados/{id}")
    public ResponseEntity<byte[]> descargar(@PathVariable String id) {
        Archivo archivo = archivoService.descargar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .body(archivo.contenido());
    }
}
