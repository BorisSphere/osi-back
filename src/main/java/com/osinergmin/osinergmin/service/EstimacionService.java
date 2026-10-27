package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.EstimacionResponse;
import com.osinergmin.osinergmin.exception.EstadoInvalidoException;
import com.osinergmin.osinergmin.exception.RequerimientoNoEncontradoException;
import com.osinergmin.osinergmin.model.AccionAuditoria;
import com.osinergmin.osinergmin.model.Estimacion;
import com.osinergmin.osinergmin.model.Requerimiento;
import com.osinergmin.osinergmin.repository.EstimacionRepository;
import com.osinergmin.osinergmin.repository.RequerimientoRepository;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository.Estado;
import com.osinergmin.osinergmin.repository.UsuarioRepository;
import com.osinergmin.osinergmin.service.EstimacionCalculator.Distribucion;
import com.osinergmin.osinergmin.util.CodigoRequerimiento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstimacionService {

    /** Se puede (re)estimar mientras no haya decisión, o tras un rechazo ("corregir y reenviar"). */
    private static final Set<Estado> ESTIMABLES = Set.of(Estado.REGISTRADO, Estado.ESTIMADO, Estado.RECHAZADO);

    private final EstimacionRepository estimacionRepository;
    private final RequerimientoRepository requerimientoDetalleRepository;
    private final RequerimientoEstadoRepository requerimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstimacionCalculator calculator;
    private final AuditoriaService auditoriaService;
    private final CorreoRequerimientoPublisher correoPublisher;

    public EstimacionService(
            EstimacionRepository estimacionRepository,
            RequerimientoRepository requerimientoDetalleRepository,
            RequerimientoEstadoRepository requerimientoRepository,
            UsuarioRepository usuarioRepository,
            EstimacionCalculator calculator,
            AuditoriaService auditoriaService,
            CorreoRequerimientoPublisher correoPublisher) {
        this.estimacionRepository = estimacionRepository;
        this.requerimientoDetalleRepository = requerimientoDetalleRepository;
        this.requerimientoRepository = requerimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.calculator = calculator;
        this.auditoriaService = auditoriaService;
        this.correoPublisher = correoPublisher;
    }

    /**
     * Calcula y guarda la estimación, pasa el requerimiento a ESTIMADO y deja el evento de auditoría.
     * Todo en una transacción: si algo falla, no queda nada a medias.
     */
    @Transactional
    public EstimacionResponse estimar(long requerimientoId, BigDecimal hhDesarrollo, long usuarioId) {
        Estado estado = requerimientoRepository.bloquearYLeerEstado(requerimientoId)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(requerimientoId));
        String codigo = CodigoRequerimiento.formatear(requerimientoId);
        if (!ESTIMABLES.contains(estado)) {
            throw new EstadoInvalidoException("No se puede estimar " + codigo + " porque está en estado " + estado + ".");
        }

        Distribucion d = calculator.calcular(hhDesarrollo);

        // UQ_LAB_EST_REQ: una sola estimación por requerimiento → si ya existe, se actualiza.
        Estimacion estimacion = estimacionRepository.findByRequerimientoId(requerimientoId).orElseGet(() -> {
            Estimacion nueva = new Estimacion();
            nueva.setRequerimientoId(requerimientoId);
            return nueva;
        });
        estimacion.setHhAnalisis(d.analisis());
        estimacion.setHhDesarrollo(d.desarrollo());
        estimacion.setHhPruebas(d.pruebas());
        estimacion.setHhDocumentacion(d.documentacion());
        estimacion.setHhTotal(d.total());
        estimacion.setEstimadoPor(usuarioRepository.getReferenceById(usuarioId));
        estimacion.setFechaEstimacion(Instant.now());
        Estimacion guardada = estimacionRepository.save(estimacion);

        requerimientoRepository.cambiarEstado(requerimientoId, Estado.ESTIMADO);
        auditoriaService.registrar(
                requerimientoId, usuarioId, AccionAuditoria.ESTIMACION,
                "Estimó " + d.total().toPlainString() + " HH para " + codigo);

        Requerimiento requerimiento = requerimientoDetalleRepository.findWithDetalleById(requerimientoId)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(requerimientoId));
        correoPublisher.estimado(requerimiento, guardada);
        return EstimacionResponse.from(guardada, Estado.ESTIMADO.name());
    }
}
