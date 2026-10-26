package com.osinergmin.osinergmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.osinergmin.osinergmin.dto.SeguimientoResponse;
import com.osinergmin.osinergmin.exception.ValidacionNegocioException;
import com.osinergmin.osinergmin.model.Aplicacion;
import com.osinergmin.osinergmin.repository.AplicacionRepository;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository.Estado;
import com.osinergmin.osinergmin.repository.TableroRepository;
import com.osinergmin.osinergmin.repository.TableroRepository.FilaBandeja;
import com.osinergmin.osinergmin.service.ReporteExportService.Archivo;
import com.osinergmin.osinergmin.service.ReporteExportService.Formato;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Genera reportes reales con JasperReports (compila las plantillas, dibuja los gráficos, llena el
 * subreporte y el detalle, y exporta) sin base de datos: los datos se simulan. Si una plantilla
 * .jrxml o un customizer de gráficos tiene un error, falla aquí.
 */
@ExtendWith(MockitoExtension.class)
class ReporteExportServiceTest {

    @Mock private TableroService tableroService;
    @Mock private TableroRepository tableroRepository;
    @Mock private AplicacionRepository aplicacionRepository;

    private ReporteExportService service;

    @BeforeEach
    void setUp() {
        service = new ReporteExportService(tableroService, tableroRepository, aplicacionRepository);
    }

    private static Map<String, Long> estados(long reg, long est, long apr, long rec) {
        return Map.of("REGISTRADO", reg, "ESTIMADO", est, "APROBADO", apr, "RECHAZADO", rec);
    }

    private static Aplicacion aplicacion(String codigo, String nombre) {
        Aplicacion a = mock(Aplicacion.class);
        when(a.getCodigo()).thenReturn(codigo);
        when(a.getNombre()).thenReturn(nombre);
        return a;
    }

    private void conDatos() {
        when(tableroService.seguimiento(any(), any(), any())).thenReturn(new SeguimientoResponse(
                4, estados(2, 0, 1, 1), new BigDecimal("100.00"),
                List.of(new SeguimientoResponse.FilaAplicacion("LAB-001", estados(1, 0, 1, 0), new BigDecimal("100.00")),
                        new SeguimientoResponse.FilaAplicacion("LAB-002", estados(1, 0, 0, 1), BigDecimal.ZERO))));
        when(tableroRepository.detalle(any(), any(), any())).thenReturn(List.of(
                new FilaBandeja(5, "Modernizar módulo de reportes", "LAB-001", "ALTA", Estado.APROBADO,
                        new BigDecimal("100.00"), LocalDateTime.of(2026, 10, 20, 15, 0), null),
                new FilaBandeja(6, "Migrar pantallas a Angular", "LAB-001", "MEDIA", Estado.REGISTRADO,
                        null, LocalDateTime.of(2026, 10, 21, 15, 0), null),
                new FilaBandeja(7, "Optimizar consultas de inspecciones", "LAB-002", "BAJA", Estado.RECHAZADO,
                        new BigDecimal("64.10"), LocalDateTime.of(2026, 10, 22, 15, 0), "Sin presupuesto"),
                new FilaBandeja(8, "Nuevo flujo de trámites", "LAB-002", "MEDIA", Estado.REGISTRADO,
                        null, LocalDateTime.of(2026, 10, 23, 15, 0), null)));
        List<Aplicacion> aplicaciones = List.of(
                aplicacion("LAB-001", "Portal de Solicitudes Demo"), aplicacion("LAB-002", "Seguimiento de Visitas Demo"));
        when(aplicacionRepository.findAll()).thenReturn(aplicaciones);
    }

    private static String inicio(byte[] bytes, int n) {
        return new String(bytes, 0, n, StandardCharsets.ISO_8859_1);
    }

    @Test
    void generaUnPdfCompletoValido() {
        conDatos();

        Archivo a = service.seguimiento(Formato.PDF, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "LAB-001", "analista3");

        assertThat(inicio(a.contenido(), 4)).isEqualTo("%PDF");
        assertThat(a.contentType()).isEqualTo("application/pdf");
        assertThat(a.nombre()).matches("seguimiento_\\d{4}-\\d{2}-\\d{2}\\.pdf");
    }

    @Test
    void generaUnExcelValido() {
        conDatos();

        Archivo a = service.seguimiento(Formato.XLSX, null, null, null, "aprobador1");

        assertThat(inicio(a.contenido(), 2)).as("un .xlsx es un ZIP").isEqualTo("PK");
        assertThat(a.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(a.nombre()).endsWith(".xlsx");
    }

    @Test
    void sinResultadosTambienGeneraElReporte() {
        when(tableroService.seguimiento(any(), any(), any()))
                .thenReturn(new SeguimientoResponse(0, estados(0, 0, 0, 0), BigDecimal.ZERO, List.of()));
        when(tableroRepository.detalle(any(), any(), any())).thenReturn(List.of());
        when(aplicacionRepository.findAll()).thenReturn(List.of());

        Archivo pdf = service.seguimiento(Formato.PDF, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null, "admin");

        assertThat(inicio(pdf.contenido(), 4)).isEqualTo("%PDF");
    }

    @Test
    void formatoInvalidoDa400SinConsultarDatos() {
        assertThatThrownBy(() -> Formato.desde("docx"))
                .isInstanceOf(ValidacionNegocioException.class)
                .hasMessageContaining("pdf o xlsx");
        verifyNoInteractions(tableroService, tableroRepository, aplicacionRepository);
    }

    @Test
    void aceptaExcelComoSinonimoDeXlsx() {
        assertThat(Formato.desde("Excel")).isEqualTo(Formato.XLSX);
        assertThat(Formato.desde(" PDF ")).isEqualTo(Formato.PDF);
    }
}
