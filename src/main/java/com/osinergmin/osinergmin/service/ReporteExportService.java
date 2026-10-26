package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.SeguimientoResponse;
import com.osinergmin.osinergmin.exception.ValidacionNegocioException;
import com.osinergmin.osinergmin.model.Aplicacion;
import com.osinergmin.osinergmin.repository.AplicacionRepository;
import com.osinergmin.osinergmin.repository.TableroRepository;
import com.osinergmin.osinergmin.repository.TableroRepository.FilaBandeja;
import com.osinergmin.osinergmin.util.CodigoRequerimiento;
import com.osinergmin.osinergmin.util.FechasLab;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import net.sf.jasperreports.pdf.JRPdfExporter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reporte de seguimiento descargable en PDF o Excel (JasperReports): indicadores, gráficos,
 * resumen por aplicación y detalle de requerimientos. Usa los mismos datos y filtros que la
 * pantalla de Reportes (TableroService.seguimiento), así los números coinciden siempre.
 */
@Service
public class ReporteExportService {

    private static final String PLANTILLA = "/reportes/seguimiento.jrxml";
    private static final String PLANTILLA_APLICACIONES = "/reportes/seguimiento-aplicaciones.jrxml";
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** Orden y etiquetas de los estados, iguales a los de la pantalla. */
    private static final Map<String, String> ESTADOS = new LinkedHashMap<>();

    static {
        ESTADOS.put("REGISTRADO", "Registrados");
        ESTADOS.put("ESTIMADO", "Estimados");
        ESTADOS.put("APROBADO", "Aprobados");
        ESTADOS.put("RECHAZADO", "Rechazados");
    }

    public enum Formato {
        PDF("pdf", "application/pdf"),
        XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        private final String extension;
        private final String contentType;

        Formato(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public static Formato desde(String valor) {
            String v = valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
            return switch (v) {
                case "pdf" -> PDF;
                case "xlsx", "excel" -> XLSX;
                default -> throw new ValidacionNegocioException("formato", "Formato inválido: use pdf o xlsx");
            };
        }
    }

    public record Archivo(byte[] contenido, String nombre, String contentType) {}

    private final TableroService tableroService;
    private final TableroRepository tableroRepository;
    private final AplicacionRepository aplicacionRepository;

    /** Plantillas compiladas la primera vez que se usan (compilar es lo costoso). */
    private volatile JasperReport plantilla;
    private volatile JasperReport plantillaAplicaciones;

    public ReporteExportService(
            TableroService tableroService, TableroRepository tableroRepository, AplicacionRepository aplicacionRepository) {
        this.tableroService = tableroService;
        this.tableroRepository = tableroRepository;
        this.aplicacionRepository = aplicacionRepository;
    }

    @Transactional(readOnly = true)
    public Archivo seguimiento(Formato formato, LocalDate desde, LocalDate hasta, String aplicacion, String usuario) {
        // Valida los filtros (desde <= hasta) y calcula los totales en Oracle.
        SeguimientoResponse datos = tableroService.seguimiento(desde, hasta, aplicacion);
        String app = aplicacion == null || aplicacion.isBlank() ? null : aplicacion.trim();
        List<FilaBandeja> detalle = tableroRepository.detalle(
                desde == null ? null : FechasLab.utc(FechasLab.inicioDelDia(desde)),
                hasta == null ? null : FechasLab.utc(FechasLab.finDelDia(hasta)),
                app);
        Map<String, String> nombres = aplicacionRepository.findAll().stream()
                .collect(Collectors.toMap(Aplicacion::getCodigo, Aplicacion::getNombre, (a, b) -> a));

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("FILTROS", describirFiltros(desde, hasta, app, nombres));
        parametros.put("GENERADO_POR", usuario == null ? "-" : usuario);
        parametros.put("FECHA_GENERACION", LocalDateTime.now(FechasLab.ZONA).format(DIA_HORA));
        parametros.put("TOTAL", datos.total());
        parametros.put("TOTAL_REGISTRADOS", cantidad(datos.porEstado(), "REGISTRADO"));
        parametros.put("TOTAL_ESTIMADOS", cantidad(datos.porEstado(), "ESTIMADO"));
        parametros.put("TOTAL_APROBADOS", cantidad(datos.porEstado(), "APROBADO"));
        parametros.put("TOTAL_RECHAZADOS", cantidad(datos.porEstado(), "RECHAZADO"));
        parametros.put("TOTAL_HH_APROBADAS", datos.hhAprobadas());
        parametros.put("HAY_HORAS", datos.hhAprobadas().signum() > 0);
        parametros.put("ESTADOS_DS", new JRMapCollectionDataSource(filasEstados(datos)));
        parametros.put("HORAS_DS", new JRMapCollectionDataSource(filasHoras(datos)));
        parametros.put("APLICACIONES_DS", new JRMapCollectionDataSource(filasAplicaciones(datos, nombres)));
        parametros.put(JRParameter.REPORT_LOCALE, new Locale("es", "PE"));
        // En Excel no tiene sentido repetir encabezados y pies por página.
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, formato == Formato.XLSX);

        try {
            parametros.put("APLICACIONES_REPORT", compilada(PLANTILLA_APLICACIONES, true));
            JasperPrint print = JasperFillManager.fillReport(
                    compilada(PLANTILLA, false), parametros, new JRMapCollectionDataSource(filasDetalle(detalle)));
            byte[] contenido = formato == Formato.PDF ? exportarPdf(print) : exportarXlsx(print);
            String nombre = "seguimiento_" + LocalDate.now(FechasLab.ZONA) + "." + formato.extension;
            return new Archivo(contenido, nombre, formato.contentType);
        } catch (JRException ex) {
            throw new IllegalStateException("No se pudo generar el reporte de seguimiento", ex);
        }
    }

    // ---------- Datos para la plantilla ----------

    private static long cantidad(Map<String, Long> porEstado, String estado) {
        return porEstado.getOrDefault(estado, 0L);
    }

    private static List<Map<String, ?>> filasEstados(SeguimientoResponse datos) {
        return ESTADOS.entrySet().stream().<Map<String, ?>>map(e ->
                Map.of("estado", e.getValue(), "cantidad", cantidad(datos.porEstado(), e.getKey()))).toList();
    }

    /** Solo las aplicaciones con horas aprobadas, de mayor a menor (ya vienen ordenadas). */
    private static List<Map<String, ?>> filasHoras(SeguimientoResponse datos) {
        return datos.porAplicacion().stream()
                .filter(f -> f.hhAprobadas().signum() > 0)
                .<Map<String, ?>>map(f -> Map.of("aplicacion", f.aplicacion(), "horas", f.hhAprobadas()))
                .toList();
    }

    private static List<Map<String, ?>> filasAplicaciones(SeguimientoResponse datos, Map<String, String> nombres) {
        long total = datos.total();
        return datos.porAplicacion().stream().<Map<String, ?>>map(f -> {
            long totalApp = f.porEstado().values().stream().mapToLong(Long::longValue).sum();
            Map<String, Object> fila = new HashMap<>();
            fila.put("codigo", f.aplicacion());
            fila.put("nombre", nombres.get(f.aplicacion()));
            fila.put("registrados", cantidad(f.porEstado(), "REGISTRADO"));
            fila.put("estimados", cantidad(f.porEstado(), "ESTIMADO"));
            fila.put("aprobados", cantidad(f.porEstado(), "APROBADO"));
            fila.put("rechazados", cantidad(f.porEstado(), "RECHAZADO"));
            fila.put("total", totalApp);
            // Redondeado aquí (6,25 % → 6,3 %): así el PDF y el Excel muestran lo mismo.
            fila.put("porcentaje", total == 0 ? 0d
                    : BigDecimal.valueOf(totalApp).divide(BigDecimal.valueOf(total), 3, RoundingMode.HALF_UP).doubleValue());
            fila.put("hhAprobadas", f.hhAprobadas());
            return fila;
        }).toList();
    }

    private static List<Map<String, ?>> filasDetalle(List<FilaBandeja> detalle) {
        return detalle.stream().<Map<String, ?>>map(f -> {
            Map<String, Object> fila = new HashMap<>();
            fila.put("codigo", CodigoRequerimiento.formatear(f.id()));
            fila.put("titulo", f.titulo());
            fila.put("aplicacion", f.aplicacion());
            fila.put("prioridad", capitalizar(f.prioridad()));
            fila.put("estado", capitalizar(f.estado().name()));
            fila.put("hhTotal", f.hhTotal());
            fila.put("fecha", f.fechaCreacionUtc() == null ? "" : FechasLab.diaEnPeru(f.fechaCreacionUtc()).format(DIA));
            return fila;
        }).toList();
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return texto.charAt(0) + texto.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String describirFiltros(LocalDate desde, LocalDate hasta, String aplicacion, Map<String, String> nombres) {
        String app = aplicacion == null ? "Todas"
                : aplicacion + (nombres.containsKey(aplicacion) ? " · " + nombres.get(aplicacion) : "");
        return "Desde " + (desde == null ? "el inicio" : desde.format(DIA))
                + "  ·  Hasta " + (hasta == null ? "hoy" : hasta.format(DIA))
                + "  ·  Aplicación: " + app;
    }

    // ---------- Compilación y exportación ----------

    private JasperReport compilada(String ruta, boolean subreporte) throws JRException {
        JasperReport actual = subreporte ? plantillaAplicaciones : plantilla;
        if (actual != null) {
            return actual;
        }
        synchronized (this) {
            actual = subreporte ? plantillaAplicaciones : plantilla;
            if (actual == null) {
                try (InputStream jrxml = getClass().getResourceAsStream(ruta)) {
                    if (jrxml == null) {
                        throw new IllegalStateException("No se encontró la plantilla " + ruta);
                    }
                    actual = JasperCompileManager.compileReport(jrxml);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                if (subreporte) {
                    plantillaAplicaciones = actual;
                } else {
                    plantilla = actual;
                }
            }
            return actual;
        }
    }

    private static byte[] exportarPdf(JasperPrint print) throws JRException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        exporter.exportReport();
        return out.toByteArray();
    }

    private static byte[] exportarXlsx(JasperPrint print) throws JRException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
        config.setOnePagePerSheet(false);
        config.setDetectCellType(true); // números como números, para poder sumarlos en Excel
        config.setRemoveEmptySpaceBetweenRows(true);
        config.setRemoveEmptySpaceBetweenColumns(true);
        config.setWhitePageBackground(false);
        config.setSheetNames(new String[] {"Seguimiento"});
        exporter.setConfiguration(config);
        exporter.exportReport();
        return out.toByteArray();
    }
}
