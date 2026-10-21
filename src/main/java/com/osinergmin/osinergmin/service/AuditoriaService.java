package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.AuditoriaResponse;
import com.osinergmin.osinergmin.exception.RequerimientoNoEncontradoException;
import com.osinergmin.osinergmin.exception.ValidacionNegocioException;
import com.osinergmin.osinergmin.model.AccionAuditoria;
import com.osinergmin.osinergmin.model.Auditoria;
import com.osinergmin.osinergmin.repository.AuditoriaRepository;
import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository;
import com.osinergmin.osinergmin.repository.UsuarioRepository;
import com.osinergmin.osinergmin.util.FechasLab;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private static final int DETALLE_MAX = 1000; // LAB_AUDITORIA.DETALLE VARCHAR2(1000 CHAR)

    private final AuditoriaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final RequerimientoEstadoRepository requerimientoRepository;

    public AuditoriaService(
            AuditoriaRepository repository,
            UsuarioRepository usuarioRepository,
            RequerimientoEstadoRepository requerimientoRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.requerimientoRepository = requerimientoRepository;
    }

    /** Registra un evento dentro de la transacción del llamador (si falla la operación, no queda el evento). */
    @Transactional
    public void registrar(long requerimientoId, long actorId, AccionAuditoria accion, String detalle) {
        Auditoria evento = new Auditoria();
        evento.setRequerimientoId(requerimientoId);
        evento.setActor(usuarioRepository.getReferenceById(actorId));
        evento.setAccion(accion);
        evento.setDetalle(detalle != null && detalle.length() > DETALLE_MAX ? detalle.substring(0, DETALLE_MAX) : detalle);
        evento.setFecha(Instant.now());
        repository.save(evento);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> historialDeRequerimiento(long requerimientoId) {
        if (!requerimientoRepository.existe(requerimientoId)) {
            throw new RequerimientoNoEncontradoException(requerimientoId);
        }
        return repository.historialDeRequerimiento(requerimientoId).stream().map(AuditoriaResponse::from).toList();
    }

    /** Historial global. desde y hasta son días completos e inclusivos; cualquiera puede omitirse. */
    @Transactional(readOnly = true)
    public List<AuditoriaResponse> buscar(LocalDate desde, LocalDate hasta, String usuario) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ValidacionNegocioException("desde", "La fecha \"desde\" no puede ser posterior a \"hasta\"");
        }
        Instant inicio = desde == null ? null : FechasLab.inicioDelDia(desde);
        Instant fin = hasta == null ? null : FechasLab.finDelDia(hasta);
        String filtroUsuario = usuario == null || usuario.isBlank() ? null : usuario.trim();
        return repository.buscar(inicio, fin, filtroUsuario).stream().map(AuditoriaResponse::from).toList();
    }
}
