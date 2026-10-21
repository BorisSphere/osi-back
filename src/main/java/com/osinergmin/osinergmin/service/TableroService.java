package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.DashboardResumenResponse;
import com.osinergmin.osinergmin.dto.RequerimientoBandejaResponse;
import com.osinergmin.osinergmin.dto.SeguimientoResponse;
import com.osinergmin.osinergmin.exception.ValidacionNegocioException;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository.Estado;
import com.osinergmin.osinergmin.repository.TableroRepository;
import com.osinergmin.osinergmin.repository.TableroRepository.FilaBandeja;
import com.osinergmin.osinergmin.repository.TableroRepository.FilaSeguimiento;
import com.osinergmin.osinergmin.util.CodigoRequerimiento;
import com.osinergmin.osinergmin.util.FechasLab;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bandeja de estimación/aprobación, dashboard (RF08) y seguimiento (RF08). Solo lectura. */
@Service
public class TableroService {

    private final TableroRepository repository;

    public TableroService(TableroRepository repository) {
        this.repository = repository;
    }

    /** Requerimientos en los estados pedidos (vacío o null = todos). */
    @Transactional(readOnly = true)
    public List<RequerimientoBandejaResponse> bandeja(Collection<String> estados) {
        return repository.bandeja(parsearEstados(estados)).stream().map(TableroService::aBandeja).toList();
    }

    @Transactional(readOnly = true)
    public DashboardResumenResponse resumen() {
        Map<String, Long> porEstado = completarEstados(repository.contarPorEstado());
        long total = porEstado.values().stream().mapToLong(Long::longValue).sum();
        List<DashboardResumenResponse.PorAplicacion> porAplicacion = repository.contarPorAplicacion().stream()
                .map(c -> new DashboardResumenResponse.PorAplicacion(c.aplicacion(), c.cantidad()))
                .toList();
        return new DashboardResumenResponse(
                repository.contarAplicaciones(), total, porEstado, repository.sumarHhAprobadas(), porAplicacion);
    }

    /** desde y hasta son días completos e inclusivos sobre la fecha de creación; todos los filtros son opcionales. */
    @Transactional(readOnly = true)
    public SeguimientoResponse seguimiento(LocalDate desde, LocalDate hasta, String aplicacion) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ValidacionNegocioException("desde", "La fecha \"desde\" no puede ser posterior a \"hasta\"");
        }
        List<FilaSeguimiento> filas = repository.seguimiento(
                desde == null ? null : FechasLab.utc(FechasLab.inicioDelDia(desde)),
                hasta == null ? null : FechasLab.utc(FechasLab.finDelDia(hasta)),
                aplicacion == null || aplicacion.isBlank() ? null : aplicacion.trim());

        // Las filas vienen por (aplicación, estado): se agrupan por aplicación y se suman los totales.
        Map<String, Map<Estado, Long>> conteoPorApp = new LinkedHashMap<>();
        Map<String, BigDecimal> hhPorApp = new LinkedHashMap<>();
        Map<Estado, Long> conteoTotal = new LinkedHashMap<>();
        BigDecimal hhTotal = BigDecimal.ZERO;
        for (FilaSeguimiento f : filas) {
            conteoPorApp.computeIfAbsent(f.aplicacion(), k -> new LinkedHashMap<>()).merge(f.estado(), f.cantidad(), Long::sum);
            hhPorApp.merge(f.aplicacion(), f.hhAprobadas(), BigDecimal::add);
            conteoTotal.merge(f.estado(), f.cantidad(), Long::sum);
            hhTotal = hhTotal.add(f.hhAprobadas());
        }

        List<SeguimientoResponse.FilaAplicacion> porAplicacion = conteoPorApp.entrySet().stream()
                .map(e -> new SeguimientoResponse.FilaAplicacion(
                        e.getKey(), completarEstados(e.getValue()), hhPorApp.get(e.getKey())))
                .sorted(Comparator.comparing(SeguimientoResponse.FilaAplicacion::hhAprobadas).reversed()
                        .thenComparing(SeguimientoResponse.FilaAplicacion::aplicacion))
                .toList();
        Map<String, Long> porEstado = completarEstados(conteoTotal);
        long total = porEstado.values().stream().mapToLong(Long::longValue).sum();
        return new SeguimientoResponse(total, porEstado, hhTotal, porAplicacion);
    }

    private static RequerimientoBandejaResponse aBandeja(FilaBandeja f) {
        return new RequerimientoBandejaResponse(
                f.id(),
                CodigoRequerimiento.formatear(f.id()),
                f.titulo(),
                f.aplicacion(),
                f.prioridad(),
                f.estado().name(),
                f.hhTotal(),
                f.fechaCreacionUtc() == null ? null : FechasLab.diaEnPeru(f.fechaCreacionUtc()),
                // Un motivo viejo no aplica si el requerimiento se reestimó o aprobó después.
                f.estado() == Estado.RECHAZADO ? f.ultimoMotivoRechazo() : null);
    }

    /** Los cuatro estados siempre presentes y en orden, con 0 si no hay ninguno. */
    private static Map<String, Long> completarEstados(Map<Estado, Long> conteo) {
        Map<String, Long> completo = new LinkedHashMap<>();
        for (Estado e : Estado.values()) {
            completo.put(e.name(), conteo.getOrDefault(e, 0L));
        }
        return completo;
    }

    private static Set<Estado> parsearEstados(Collection<String> estados) {
        Set<Estado> resultado = EnumSet.noneOf(Estado.class);
        if (estados == null) {
            return resultado;
        }
        for (String s : estados) {
            try {
                resultado.add(Estado.valueOf(s.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new ValidacionNegocioException(
                        "estado", "Estado inválido: " + s + " (use REGISTRADO, ESTIMADO, APROBADO o RECHAZADO)");
            }
        }
        return resultado;
    }
}
