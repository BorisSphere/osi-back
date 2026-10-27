package com.osinergmin.osinergmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.osinergmin.osinergmin.dto.EstimacionResponse;
import com.osinergmin.osinergmin.exception.EstadoInvalidoException;
import com.osinergmin.osinergmin.exception.RequerimientoNoEncontradoException;
import com.osinergmin.osinergmin.model.AccionAuditoria;
import com.osinergmin.osinergmin.model.Estimacion;
import com.osinergmin.osinergmin.repository.EstimacionRepository;
import com.osinergmin.osinergmin.repository.RequerimientoRepository;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository.Estado;
import com.osinergmin.osinergmin.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reglas de la estimación sin base de datos (los repositorios se simulan con Mockito). */
@ExtendWith(MockitoExtension.class)
class EstimacionServiceTest {

    private static final long REQ = 1L;
    private static final long ANALISTA = 7L;

    @Mock private EstimacionRepository estimacionRepository;
    @Mock private RequerimientoRepository requerimientoDetalleRepository;
    @Mock private RequerimientoEstadoRepository requerimientoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CorreoRequerimientoPublisher correoPublisher;

    private EstimacionService service;

    @BeforeEach
    void setUp() {
        service = new EstimacionService(
                estimacionRepository, requerimientoDetalleRepository, requerimientoRepository,
                usuarioRepository, new EstimacionCalculator(), auditoriaService, correoPublisher);
    }

    private void guardarDevuelveLoMismo() {
        when(estimacionRepository.save(any(Estimacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void requerimientoRegistradoSeEstimaPasaAEstimadoYSeAudita() {
        when(requerimientoRepository.bloquearYLeerEstado(REQ)).thenReturn(Optional.of(Estado.REGISTRADO));
        when(estimacionRepository.findByRequerimientoId(REQ)).thenReturn(Optional.empty());
        when(requerimientoDetalleRepository.findWithDetalleById(REQ))
                .thenReturn(Optional.of(new com.osinergmin.osinergmin.model.Requerimiento()));
        guardarDevuelveLoMismo();

        EstimacionResponse r = service.estimar(REQ, new BigDecimal("78"), ANALISTA);

        assertThat(r.codigo()).isEqualTo("REQ-0001");
        assertThat(r.estado()).isEqualTo("ESTIMADO");
        assertThat(r.hhTotal()).isEqualByComparingTo("100.00");
        assertThat(r.hhDocumentacion()).isEqualByComparingTo("2.00");
        verify(requerimientoRepository).cambiarEstado(REQ, Estado.ESTIMADO);
        verify(auditoriaService).registrar(REQ, ANALISTA, AccionAuditoria.ESTIMACION, "Estimó 100.00 HH para REQ-0001");
    }

    @Test
    void siYaTeniaEstimacionSeActualizaLaMismaFila() {
        Estimacion existente = new Estimacion();
        existente.setId(55L);
        existente.setRequerimientoId(REQ);
        when(requerimientoRepository.bloquearYLeerEstado(REQ)).thenReturn(Optional.of(Estado.ESTIMADO));
        when(estimacionRepository.findByRequerimientoId(REQ)).thenReturn(Optional.of(existente));
        when(requerimientoDetalleRepository.findWithDetalleById(REQ))
                .thenReturn(Optional.of(new com.osinergmin.osinergmin.model.Requerimiento()));
        guardarDevuelveLoMismo();

        service.estimar(REQ, new BigDecimal("50"), ANALISTA);

        ArgumentCaptor<Estimacion> guardada = ArgumentCaptor.forClass(Estimacion.class);
        verify(estimacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getId()).isEqualTo(55L); // misma fila, no una nueva
        assertThat(guardada.getValue().getHhTotal()).isEqualByComparingTo("64.10");
    }

    @Test
    void requerimientoRechazadoPuedeCorregirseYReestimarse() {
        when(requerimientoRepository.bloquearYLeerEstado(REQ)).thenReturn(Optional.of(Estado.RECHAZADO));
        when(estimacionRepository.findByRequerimientoId(REQ)).thenReturn(Optional.empty());
        when(requerimientoDetalleRepository.findWithDetalleById(REQ))
                .thenReturn(Optional.of(new com.osinergmin.osinergmin.model.Requerimiento()));
        guardarDevuelveLoMismo();

        EstimacionResponse r = service.estimar(REQ, new BigDecimal("78"), ANALISTA);

        assertThat(r.estado()).isEqualTo("ESTIMADO");
    }

    @Test
    void requerimientoAprobadoNoSePuedeEstimarYNoSeGuardaNada() {
        when(requerimientoRepository.bloquearYLeerEstado(REQ)).thenReturn(Optional.of(Estado.APROBADO));

        assertThatThrownBy(() -> service.estimar(REQ, new BigDecimal("78"), ANALISTA))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("APROBADO");

        verify(estimacionRepository, never()).save(any());
        verify(requerimientoRepository, never()).cambiarEstado(anyLong(), any());
        verify(auditoriaService, never()).registrar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void requerimientoInexistenteDa404() {
        when(requerimientoRepository.bloquearYLeerEstado(eq(99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.estimar(99L, new BigDecimal("78"), ANALISTA))
                .isInstanceOf(RequerimientoNoEncontradoException.class);

        verify(estimacionRepository, never()).save(any());
    }
}
