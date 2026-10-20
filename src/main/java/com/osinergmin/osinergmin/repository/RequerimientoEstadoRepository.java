package com.osinergmin.osinergmin.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso mínimo a LAB_REQUERIMIENTO para los flujos de estimación y aprobación: leer el estado y
 * cambiarlo. Usa SQL directo para no duplicar la entidad del módulo de requerimientos.
 */
@Repository
public class RequerimientoEstadoRepository {

    /** Valores de CK_LAB_REQ_ESTADO. */
    public enum Estado {
        REGISTRADO,
        ESTIMADO,
        APROBADO,
        RECHAZADO
    }

    private final NamedParameterJdbcTemplate jdbc;

    public RequerimientoEstadoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Estado actual, bloqueando la fila hasta el fin de la transacción (SELECT ... FOR UPDATE):
     * así dos usuarios no pueden estimar o decidir el mismo requerimiento a la vez.
     */
    public Optional<Estado> bloquearYLeerEstado(long id) {
        List<String> estados = jdbc.queryForList(
                "SELECT ESTADO FROM LAB_REQUERIMIENTO WHERE ID = :id FOR UPDATE", Map.of("id", id), String.class);
        return estados.stream().findFirst().map(Estado::valueOf);
    }

    public boolean existe(long id) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM LAB_REQUERIMIENTO WHERE ID = :id", Map.of("id", id), Integer.class);
        return n != null && n > 0;
    }

    /** Cambia el estado y actualiza FECHA_ACTUALIZACION (la tabla no tiene trigger). */
    public void cambiarEstado(long id, Estado nuevo) {
        jdbc.update(
                "UPDATE LAB_REQUERIMIENTO SET ESTADO = :estado, FECHA_ACTUALIZACION = SYSTIMESTAMP WHERE ID = :id",
                Map.of("estado", nuevo.name(), "id", id));
    }
}
