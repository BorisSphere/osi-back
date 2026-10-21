package com.osinergmin.osinergmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.osinergmin.osinergmin.dto.DecisionRequest;
import com.osinergmin.osinergmin.dto.DecisionRequest.Decision;
import com.osinergmin.osinergmin.dto.DecisionResponse;
import com.osinergmin.osinergmin.exception.EstadoInvalidoException;
import com.osinergmin.osinergmin.exception.RequerimientoNoEncontradoException;
import com.osinergmin.osinergmin.exception.ValidacionNegocioException;
import com.osinergmin.osinergmin.model.AccionAuditoria;
import com.osinergmin.osinergmin.model.Estimacion;
import com.osinergmin.osinergmin.repository.EstimacionRepository;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository.Estado;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reglas de aprobación y rechazo (CA07, CA08) sin base de datos. El 403 de CA02 lo aplica @PreAuthorize. */
@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    private static final long REQ = 1L;
    private static final long APROBADOR = 9L;

    @Mock private RequerimientoEstadoRepository requerimientoRepository;
    @Mock private EstimacionRepository estimacionRepository;
    @Mock private AuditoriaService auditoriaService;

    private DecisionService service;

    @BeforeEach
    void setUp() {
        service = new DecisionService(requerimientoRepository, estimacionRepository, auditoriaService);
    }

    private void estimadoCon(String hhTotal) {
        Estimacion e = new Estimacion();
        e.setRequerimientoId(REQ);
        e.setHhTotal(new BigDecimal(hhTotal));
        when(requerimientoRepository.bloquearYLeerEstado(REQ)).thenReturn(Optional.of(Estado.ESTIMADO));
        when(estimacionRepository.findByRequerimientoId(REQ)).thenReturn(Optional.of(e));
    }

    private void verificarQueNoCambioNada() {
        verify(requerimientoRepository, never()).cambiarEstado(anyLong(), any());
        verify(auditoriaService, never()).registrar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void aprobarUnRequerimientoEstimado() {
        estimadoCon("100.00");

        DecisionResponse r = service.decidir(REQ, new DecisionRequest(Decision.APROBADO, null), APROBADOR);

        assertThat(r.estado()).isEqualTo("APROBADO");
        assertThat(r.motivoRechazo()).isNull();
        verify(requerimientoRepository).cambiarEstado(REQ, Estado.APROBADO);
        verify(auditoriaService).registrar(REQ, APROBADOR, AccionAuditoria.APROBACION, "Aprobó REQ-0001 con 100.00 HH");
    }

    @Test
    void rechazarConMotivoGuardaElMotivoEnLaAuditoria() {
        estimadoCon("100.00");

        DecisionResponse r = service.decidir(
                REQ, new DecisionRequest(Decision.RECHAZADO, "  No hay presupuesto este trimestre  "), APROBADOR);

        assertThat(r.estado()).isEqualTo("RECHAZADO");
        assertThat(r.motivoRechazo()).isEqualTo("No hay presupuesto este trimestre");
        verify(requerimientoRepository).cambiarEstado(REQ, Estado.RECHAZADO);
        verify(auditoriaService).registrar(REQ, APROBADOR, AccionAuditoria.RECHAZO, "No hay presupuesto este trimestre");
    }

    @Test
    void ca08_rechazarSinMotivoDa400YNoTocaLaBase() {
        assertThatThrownBy(() -> service.decidir(REQ, new DecisionRequest(Decision.RECHAZADO, "   "), APROBADOR))
                .isInstanceOf(ValidacionNegocioException.class)
                .hasMessageContaining("motivo");

        verifyNoInteractions(requerimientoRepository, estimacionRepository, auditoriaService);
    }

    @Test
    void ca08_motivoDemasiadoCortoDa400() {
        assertThatThrownBy(() -> service.decidir(REQ, new DecisionRequest(Decision.RECHAZADO, "no"), APROBADOR))
                .isInstanceOf(ValidacionNegocioException.class);
    }

    @Test
    void ca07_soloSeApruebaUnRequerimientoEstimado() {
        when(requerimientoRepository.bloquearYLeerEstado(REQ)).thenReturn(Optional.of(Estado.REGISTRADO));

        assertThatThrownBy(() -> service.decidir(REQ, new DecisionRequest(Decision.APROBADO, null), APROBADOR))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("REGISTRADO");

        verificarQueNoCambioNada();
    }

    @Test
    void unRequerimientoYaAprobadoNoSePuedeDecidirDeNuevo() {
        when(requerimientoRepository.bloquearYLeerEstado(REQ)).thenReturn(Optional.of(Estado.APROBADO));

        assertThatThrownBy(() -> service.decidir(REQ, new DecisionRequest(Decision.RECHAZADO, "Cambio de prioridades"), APROBADOR))
                .isInstanceOf(EstadoInvalidoException.class);

        verificarQueNoCambioNada();
    }

    @Test
    void estimadoSinEstimacionGuardadaNoSeAprueba() {
        when(requerimientoRepository.bloquearYLeerEstado(REQ)).thenReturn(Optional.of(Estado.ESTIMADO));
        when(estimacionRepository.findByRequerimientoId(REQ)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decidir(REQ, new DecisionRequest(Decision.APROBADO, null), APROBADOR))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("estimación positiva");

        verificarQueNoCambioNada();
    }

    @Test
    void requerimientoInexistenteDa404() {
        when(requerimientoRepository.bloquearYLeerEstado(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decidir(99L, new DecisionRequest(Decision.APROBADO, null), APROBADOR))
                .isInstanceOf(RequerimientoNoEncontradoException.class);
    }
}
