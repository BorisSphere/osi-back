package com.osinergmin.osinergmin.repository;

import com.osinergmin.osinergmin.model.Auditoria;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /** Historial de un requerimiento, del más reciente al más antiguo (usa IX_LAB_AUD_REQ). */
    @Query("""
            select a from Auditoria a join fetch a.actor
            where a.requerimientoId = :requerimientoId
            order by a.fecha desc, a.id desc""")
    List<Auditoria> historialDeRequerimiento(@Param("requerimientoId") Long requerimientoId);

    /** Historial global con filtros opcionales (null = sin filtro). hasta es exclusivo. */
    @Query("""
            select a from Auditoria a join fetch a.actor u
            where (:desde is null or a.fecha >= :desde)
              and (:hasta is null or a.fecha < :hasta)
              and (:usuario is null or lower(u.username) = lower(:usuario))
            order by a.fecha desc, a.id desc""")
    List<Auditoria> buscar(
            @Param("desde") Instant desde, @Param("hasta") Instant hasta, @Param("usuario") String usuario);
}
