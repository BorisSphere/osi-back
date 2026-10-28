package com.osinergmin.osinergmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.osinergmin.osinergmin.alfresco.AlfrescoClient;
import com.osinergmin.osinergmin.alfresco.AlfrescoNoDisponibleException;
import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Contenido;
import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Elemento;
import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Nodo;
import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Ruta;
import com.osinergmin.osinergmin.dto.ReporteArchivadoResponse;
import com.osinergmin.osinergmin.exception.ReporteArchivadoNoEncontradoException;
import com.osinergmin.osinergmin.service.ReporteExportService.Archivo;
import com.osinergmin.osinergmin.service.ReporteExportService.Formato;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

/** Reglas del archivo en Alfresco (el cliente de Alfresco se simula). */
@ExtendWith(MockitoExtension.class)
class ReporteArchivoServiceTest {

    @Mock private ReporteExportService exportService;
    @Mock private AlfrescoClient alfresco;

    private ReporteArchivoService service;

    @BeforeEach
    void setUp() {
        service = new ReporteArchivoService(exportService, alfresco);
    }

    private static Nodo archivo(String id, String nombre, String creado, String... rutaIds) {
        List<Elemento> elementos = java.util.Arrays.stream(rutaIds).map(r -> new Elemento(r, r)).toList();
        return new Nodo(id, nombre, false, true, creado, new Contenido("application/pdf", 10L),
                Map.of("cm:title", "Reporte", "cm:description", "Filtros"), new Ruta("/x", elementos));
    }

    @Test
    void archivaElPdfEnLaCarpetaDelAnioYMesConQuienLoArchivo() {
        when(alfresco.carpetaReportes()).thenReturn("R1");
        when(exportService.seguimiento(eq(Formato.PDF), any(), any(), eq("LAB-001"), eq("analista3")))
                .thenReturn(new Archivo(new byte[] {1}, "x.pdf", "application/pdf"));
        when(alfresco.obtenerOCrearCarpeta(eq("R1"), matches("\\d{4}"))).thenReturn("A1");
        when(alfresco.obtenerOCrearCarpeta(eq("A1"), matches("\\d{2}"))).thenReturn("M1");
        when(alfresco.subir(eq("M1"), matches("seguimiento_\\d{4}-\\d{2}-\\d{2}_\\d{6}\\.pdf"), any(), eq(MediaType.APPLICATION_PDF),
                anyString(), matches(".*Aplicación: LAB-001.*Archivado por analista3")))
                .thenReturn(archivo("D1", "seguimiento.pdf", "2026-10-27T21:34:07.210+0000", "R1", "A1", "M1"));

        ReporteArchivadoResponse r = service.archivar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "LAB-001", "analista3");

        assertThat(r.id()).isEqualTo("D1");
        assertThat(r.titulo()).isEqualTo("Reporte");
        assertThat(r.tamanio()).isEqualTo(10L);
    }

    @Test
    void siAlfrescoNoEstaDisponibleNoGeneraElPdf() {
        when(alfresco.carpetaReportes()).thenThrow(new AlfrescoNoDisponibleException("Alfresco no responde"));

        assertThatThrownBy(() -> service.archivar(null, null, null, "admin")).isInstanceOf(AlfrescoNoDisponibleException.class);
        verifyNoInteractions(exportService);
    }

    @Test
    void listaLosReportesDelMasRecienteAlMasAntiguo() {
        when(alfresco.carpetaReportes()).thenReturn("R1");
        when(alfresco.hijos("R1", true)).thenReturn(List.of(archivo("A1", "2026", null)));
        when(alfresco.hijos("A1", true)).thenReturn(List.of(archivo("M9", "09", null), archivo("M10", "10", null)));
        when(alfresco.hijos("M9", false)).thenReturn(List.of(archivo("D1", "viejo.pdf", "2026-09-01T10:00:00.000+0000")));
        when(alfresco.hijos("M10", false)).thenReturn(List.of(archivo("D2", "nuevo.pdf", "2026-10-27T10:00:00.000+0000")));

        assertThat(service.listar()).extracting(ReporteArchivadoResponse::id).containsExactly("D2", "D1");
    }

    @Test
    void descargaSoloDocumentosDentroDeLaCarpetaDeReportes() {
        when(alfresco.carpetaReportes()).thenReturn("R1");
        when(alfresco.nodoConRuta("D1")).thenReturn(Optional.of(archivo("D1", "rep.pdf", null, "ROOT", "R1", "A1", "M1")));
        when(alfresco.contenido("D1")).thenReturn(new byte[] {7});

        Archivo a = service.descargar("D1");

        assertThat(a.nombre()).isEqualTo("rep.pdf");
        assertThat(a.contenido()).containsExactly(7);
    }

    @Test
    void unDocumentoFueraDeReportesDa404YNoSeDescarga() {
        when(alfresco.carpetaReportes()).thenReturn("R1");
        when(alfresco.nodoConRuta("OTRO")).thenReturn(Optional.of(archivo("OTRO", "contrato.pdf", null, "ROOT", "LEGAL")));

        assertThatThrownBy(() -> service.descargar("OTRO")).isInstanceOf(ReporteArchivadoNoEncontradoException.class);
        verify(alfresco, never()).contenido(anyString());
    }

    @Test
    void unIdInexistenteDa404() {
        when(alfresco.carpetaReportes()).thenReturn("R1");
        when(alfresco.nodoConRuta("NADA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descargar("NADA")).isInstanceOf(ReporteArchivadoNoEncontradoException.class);
    }
}
