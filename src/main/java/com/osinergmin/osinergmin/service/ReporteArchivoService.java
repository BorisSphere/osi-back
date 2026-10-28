package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.alfresco.AlfrescoClient;
import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Elemento;
import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Nodo;
import com.osinergmin.osinergmin.dto.ReporteArchivadoResponse;
import com.osinergmin.osinergmin.exception.ReporteArchivadoNoEncontradoException;
import com.osinergmin.osinergmin.service.ReporteExportService.Archivo;
import com.osinergmin.osinergmin.service.ReporteExportService.Formato;
import com.osinergmin.osinergmin.util.FechasLab;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * Archivo de reportes de seguimiento en Alfresco: Compartido/Reportes/{año}/{mes}/.
 * Se archiva solo el PDF (documento oficial). El usuario técnico de Alfresco figura como creador;
 * el usuario del sistema que lo archivó queda en la descripción del documento.
 */
@Service
public class ReporteArchivoService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final ReporteExportService exportService;
    private final AlfrescoClient alfresco;

    public ReporteArchivoService(ReporteExportService exportService, AlfrescoClient alfresco) {
        this.exportService = exportService;
        this.alfresco = alfresco;
    }

    public ReporteArchivadoResponse archivar(LocalDate desde, LocalDate hasta, String aplicacion, String usuario) {
        // Primero se valida la conexión: si Alfresco no está, no se gasta en generar el PDF.
        String raiz = alfresco.carpetaReportes();
        Archivo pdf = exportService.seguimiento(Formato.PDF, desde, hasta, aplicacion, usuario);

        LocalDateTime ahora = LocalDateTime.now(FechasLab.ZONA);
        String anio = alfresco.obtenerOCrearCarpeta(raiz, String.valueOf(ahora.getYear()));
        String mes = alfresco.obtenerOCrearCarpeta(anio, String.format("%02d", ahora.getMonthValue()));

        String nombre = "seguimiento_" + ahora.format(SELLO) + ".pdf";
        String titulo = "Reporte de seguimiento " + ahora.format(DIA_HORA);
        String descripcion = describir(desde, hasta, aplicacion) + " · Archivado por " + usuario;
        return aRespuesta(alfresco.subir(mes, nombre, pdf.contenido(), MediaType.APPLICATION_PDF, titulo, descripcion));
    }

    /** Reportes archivados, del más reciente al más antiguo (recorre las carpetas de año y mes). */
    public List<ReporteArchivadoResponse> listar() {
        String raiz = alfresco.carpetaReportes();
        List<ReporteArchivadoResponse> reportes = new ArrayList<>();
        for (Nodo anio : alfresco.hijos(raiz, true)) {
            for (Nodo mes : alfresco.hijos(anio.id(), true)) {
                alfresco.hijos(mes.id(), false).forEach(n -> reportes.add(aRespuesta(n)));
            }
        }
        reportes.sort(Comparator.comparing(ReporteArchivadoResponse::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return reportes;
    }

    /**
     * Contenido de un reporte archivado. Solo se entregan documentos que están dentro de la carpeta
     * de reportes: no se puede usar este endpoint para leer otros archivos de Alfresco.
     */
    public Archivo descargar(String id) {
        String raiz = alfresco.carpetaReportes();
        Nodo nodo = alfresco.nodoConRuta(id).orElseThrow(() -> new ReporteArchivadoNoEncontradoException(id));
        boolean enReportes = nodo.path() != null && nodo.path().elements() != null
                && nodo.path().elements().stream().map(Elemento::id).anyMatch(raiz::equals);
        if (!Boolean.TRUE.equals(nodo.isFile()) || !enReportes) {
            throw new ReporteArchivadoNoEncontradoException(id);
        }
        String tipo = nodo.content() != null && nodo.content().mimeType() != null
                ? nodo.content().mimeType() : MediaType.APPLICATION_PDF_VALUE;
        return new Archivo(alfresco.contenido(id), nodo.name(), tipo);
    }

    private static ReporteArchivadoResponse aRespuesta(Nodo n) {
        return new ReporteArchivadoResponse(
                n.id(),
                n.name(),
                n.propiedad("cm:title"),
                n.propiedad("cm:description"),
                n.creado(),
                n.content() == null ? null : n.content().sizeInBytes());
    }

    private static String describir(LocalDate desde, LocalDate hasta, String aplicacion) {
        return "Desde " + (desde == null ? "el inicio" : desde.format(DIA))
                + " · Hasta " + (hasta == null ? "hoy" : hasta.format(DIA))
                + " · Aplicación: " + (aplicacion == null || aplicacion.isBlank() ? "Todas" : aplicacion.trim());
    }
}
