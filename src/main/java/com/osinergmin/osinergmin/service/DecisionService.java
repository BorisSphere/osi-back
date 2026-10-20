package com.osinergmin.osinergmin.service;

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
import com.osinergmin.osinergmin.util.CodigoRequerimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aprobación o rechazo de requerimientos (RF06). El permiso (solo APROBADOR) lo aplica el controlador. */
@Service
public class DecisionService {

    static final int MOTIVO_MIN = 10;

    private final RequerimientoEstadoRepository requerimientoRepository;
    private final EstimacionRepository estimacionRepository;
    private final AuditoriaService auditoriaService;

    public DecisionService(
            RequerimientoEstadoRepository requerimientoRepository,
            EstimacionRepository estimacionRepository,
            AuditoriaService auditoriaService) {
        this.requerimientoRepository = requerimientoRepository;
        this.estimacionRepository = estimacionRepository;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Reglas: solo un requerimiento ESTIMADO con estimación positiva (CA07) y, al rechazar, motivo
     * obligatorio (CA08). La fila queda bloqueada durante la transacción, así dos aprobadores no
     * pueden decidir el mismo requerimiento a la vez.
     */
    @Transactional
    public DecisionResponse decidir(long requerimientoId, DecisionRequest request, long aprobadorId) {
        String motivo = request.motivo() == null ? "" : request.motivo().trim();
        boolean rechazo = request.decision() == Decision.RECHAZADO;
        if (rechazo && motivo.length() < MOTIVO_MIN) {
            throw new ValidacionNegocioException(
                    "motivo", "El motivo del rechazo es obligatorio (mínimo " + MOTIVO_MIN + " caracteres)");
        }

        Estado estado = requerimientoRepository.bloquearYLeerEstado(requerimientoId)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(requerimientoId));
        String codigo = CodigoRequerimiento.formatear(requerimientoId);
        if (estado != Estado.ESTIMADO) {
            throw new EstadoInvalidoException(
                    "Solo se puede aprobar o rechazar un requerimiento ESTIMADO; " + codigo + " está en estado " + estado + ".");
        }
        Estimacion estimacion = estimacionRepository.findByRequerimientoId(requerimientoId)
                .filter(e -> e.getHhTotal() != null && e.getHhTotal().signum() > 0)
                .orElseThrow(() -> new EstadoInvalidoException(codigo + " no tiene una estimación positiva."));

        Estado nuevo = rechazo ? Estado.RECHAZADO : Estado.APROBADO;
        requerimientoRepository.cambiarEstado(requerimientoId, nuevo);
        if (rechazo) {
            // LAB_REQUERIMIENTO no tiene columna para el motivo: queda en el DETALLE del evento RECHAZO.
            auditoriaService.registrar(requerimientoId, aprobadorId, AccionAuditoria.RECHAZO, motivo);
        } else {
            auditoriaService.registrar(requerimientoId, aprobadorId, AccionAuditoria.APROBACION,
                    "Aprobó " + codigo + " con " + estimacion.getHhTotal().toPlainString() + " HH");
        }
        return new DecisionResponse(requerimientoId, codigo, nuevo.name(), rechazo ? motivo : null);
    }
}
