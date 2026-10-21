package com.osinergmin.osinergmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.osinergmin.osinergmin.dto.DashboardResumenResponse;
import com.osinergmin.osinergmin.dto.RequerimientoBandejaResponse;
import com.osinergmin.osinergmin.dto.SeguimientoResponse;
import com.osinergmin.osinergmin.exception.ValidacionNegocioException;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository.Estado;
import com.osinergmin.osinergmin.repository.TableroRepository;
import com.osinergmin.osinergmin.repository.TableroRepository.CantidadPorAplicacion;
import com.osinergmin.osinergmin.repository.TableroRepository.FilaBandeja;
import com.osinergmin.osinergmin.repository.TableroRepository.FilaSeguimiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Armado de bandeja, dashboard y seguimiento sin base de datos. */
@ExtendWith(MockitoExtension.class)
class TableroServiceTest {

    @Mock private TableroRepository repository;

    private TableroService service;

    @BeforeEach
    void setUp() {
        service = new TableroService(repository);
    }

    // ---------- Bandeja ----------

    @Test
    void bandejaFiltraPorEstadosYArmaCodigoYFecha() {
        // 2026-10-20 03:00 UTC = 2026-10-19 22:00 en Perú → el día de registro es el 19.
        when(repository.bandeja(EnumSet.of(Estado.ESTIMADO, Estado.RECHAZADO))).thenReturn(List.of(
                new FilaBandeja(7, "Migrar", "LAB-002", "ALTA", Estado.ESTIMADO, new BigDecimal("100.00"),
                        LocalDateTime.of(2026, 10, 20, 3, 0), "motivo viejo")));

        List<RequerimientoBandejaResponse> r = service.bandeja(List.of("estimado", "RECHAZADO"));

        assertThat(r).hasSize(1);
        assertThat(r.get(0).codigo()).isEqualTo("REQ-0007");
        assertThat(r.get(0).fechaRegistro()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(r.get(0).motivoRechazo()).as("si ya no está RECHAZADO no se muestra el motivo").isNull();
    }

    @Test
    void bandejaMuestraElMotivoSoloEnRechazados() {
        when(repository.bandeja(EnumSet.noneOf(Estado.class))).thenReturn(List.of(
                new FilaBandeja(3, "Notificaciones", "LAB-003", "BAJA", Estado.RECHAZADO, new BigDecimal("40.00"),
                        LocalDateTime.of(2026, 10, 1, 15, 0), "Fuera del alcance")));

        assertThat(service.bandeja(null).get(0).motivoRechazo()).isEqualTo("Fuera del alcance");
    }

    @Test
    void bandejaConEstadoInvalidoDa400() {
        assertThatThrownBy(() -> service.bandeja(List.of("PENDIENTE")))
                .isInstanceOf(ValidacionNegocioException.class)
                .hasMessageContaining("PENDIENTE");
        verifyNoInteractions(repository);
    }

    // ---------- Dashboard ----------

    @Test
    void resumenCompletaLosCuatroEstadosYSumaElTotal() {
        when(repository.contarPorEstado()).thenReturn(Map.of(Estado.REGISTRADO, 2L, Estado.APROBADO, 3L));
        when(repository.contarAplicaciones()).thenReturn(5L);
        when(repository.sumarHhAprobadas()).thenReturn(new BigDecimal("340.00"));
        when(repository.contarPorAplicacion()).thenReturn(List.of(new CantidadPorAplicacion("LAB-001", 5)));

        DashboardResumenResponse r = service.resumen();

        assertThat(r.aplicaciones()).isEqualTo(5);
        assertThat(r.requerimientos()).isEqualTo(5);
        assertThat(r.porEstado()).containsExactly(
                Map.entry("REGISTRADO", 2L), Map.entry("ESTIMADO", 0L),
                Map.entry("APROBADO", 3L), Map.entry("RECHAZADO", 0L));
        assertThat(r.hhAprobadas()).isEqualByComparingTo("340.00");
        assertThat(r.porAplicacion()).extracting(DashboardResumenResponse.PorAplicacion::aplicacion).containsExactly("LAB-001");
    }

    // ---------- Seguimiento ----------

    @Test
    void seguimientoAgrupaPorAplicacionYOrdenaPorHorasAprobadas() {
        when(repository.seguimiento(isNull(), isNull(), isNull())).thenReturn(List.of(
                new FilaSeguimiento("LAB-001", Estado.APROBADO, 1, new BigDecimal("100.00")),
                new FilaSeguimiento("LAB-002", Estado.APROBADO, 2, new BigDecimal("240.00")),
                new FilaSeguimiento("LAB-002", Estado.ESTIMADO, 1, BigDecimal.ZERO),
                new FilaSeguimiento("LAB-003", Estado.REGISTRADO, 4, BigDecimal.ZERO)));

        SeguimientoResponse r = service.seguimiento(null, null, " ");

        assertThat(r.total()).isEqualTo(8);
        assertThat(r.hhAprobadas()).isEqualByComparingTo("340.00");
        assertThat(r.porEstado().get("APROBADO")).isEqualTo(3);
        assertThat(r.porAplicacion()).extracting(SeguimientoResponse.FilaAplicacion::aplicacion)
                .containsExactly("LAB-002", "LAB-001", "LAB-003"); // más HH aprobadas primero
        SeguimientoResponse.FilaAplicacion lab002 = r.porAplicacion().get(0);
        assertThat(lab002.porEstado().get("APROBADO")).isEqualTo(2);
        assertThat(lab002.porEstado().get("ESTIMADO")).isEqualTo(1);
        assertThat(lab002.porEstado().get("RECHAZADO")).isZero();
    }

    @Test
    void seguimientoConvierteElRangoDeDiasDePeruAUtc() {
        when(repository.seguimiento(any(), any(), any())).thenReturn(List.of());

        service.seguimiento(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "LAB-001");

        // 00:00 en Perú (UTC-5) = 05:00 UTC; "hasta" es inclusivo → límite exclusivo al día siguiente.
        verify(repository).seguimiento(
                LocalDateTime.of(2026, 10, 1, 5, 0), LocalDateTime.of(2026, 11, 1, 5, 0), "LAB-001");
    }

    @Test
    void seguimientoSinResultadosDaCerosEnTodosLosEstados() {
        when(repository.seguimiento(any(), any(), any())).thenReturn(List.of());

        SeguimientoResponse r = service.seguimiento(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null);

        assertThat(r.total()).isZero();
        assertThat(r.porEstado()).hasSize(4).allSatisfy((estado, n) -> assertThat(n).isZero());
        assertThat(r.porAplicacion()).isEmpty();
    }

    @Test
    void seguimientoConDesdePosteriorAHastaDa400() {
        assertThatThrownBy(() -> service.seguimiento(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null))
                .isInstanceOf(ValidacionNegocioException.class);
        verifyNoInteractions(repository);
    }
}
