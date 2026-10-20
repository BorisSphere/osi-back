package com.osinergmin.osinergmin.repository;

import com.osinergmin.osinergmin.repository.RequerimientoEstadoRepository.Estado;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas de solo lectura para la bandeja, el dashboard y los reportes. Los conteos y sumas se
 * calculan en Oracle (COUNT / SUM / GROUP BY) para que coincidan con la base (CA09).
 */
@Repository
public class TableroRepository {

    /** Requerimiento con su estimación y, si fue rechazado, el último motivo de rechazo. */
    public record FilaBandeja(
            long id, String titulo, String aplicacion, String prioridad, Estado estado,
            BigDecimal hhTotal, LocalDateTime fechaCreacionUtc, String ultimoMotivoRechazo) {}

    public record CantidadPorAplicacion(String aplicacion, long cantidad) {}

    /** Conteo por aplicación y estado; hhAprobadas solo suma los APROBADOS. */
    public record FilaSeguimiento(String aplicacion, Estado estado, long cantidad, BigDecimal hhAprobadas) {}

    private static final String BANDEJA = """
            SELECT r.ID, r.TITULO, a.CODIGO AS APLICACION, r.PRIORIDAD, r.ESTADO, e.HH_TOTAL, r.FECHA_CREACION,
                   (SELECT au.DETALLE FROM LAB_AUDITORIA au
                     WHERE au.REQUERIMIENTO_ID = r.ID AND au.ACCION = 'RECHAZO'
                     ORDER BY au.FECHA DESC, au.ID DESC FETCH FIRST 1 ROW ONLY) AS MOTIVO
              FROM LAB_REQUERIMIENTO r
              JOIN LAB_APLICACION a ON a.ID = r.APLICACION_ID
              LEFT JOIN LAB_ESTIMACION e ON e.REQUERIMIENTO_ID = r.ID
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public TableroRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Requerimientos en los estados indicados (vacío = todos), del más reciente al más antiguo. */
    public List<FilaBandeja> bandeja(Set<Estado> estados) {
        Map<String, Object> params = new HashMap<>();
        String sql = BANDEJA;
        if (!estados.isEmpty()) {
            sql += " WHERE r.ESTADO IN (:estados)";
            params.put("estados", estados.stream().map(Estado::name).toList());
        }
        sql += " ORDER BY r.ID DESC";
        return jdbc.query(sql, params, (rs, i) -> new FilaBandeja(
                rs.getLong("ID"),
                rs.getString("TITULO"),
                rs.getString("APLICACION"),
                rs.getString("PRIORIDAD"),
                Estado.valueOf(rs.getString("ESTADO")),
                rs.getBigDecimal("HH_TOTAL"),
                rs.getObject("FECHA_CREACION", LocalDateTime.class),
                rs.getString("MOTIVO")));
    }

    public long contarAplicaciones() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM LAB_APLICACION", Map.of(), Long.class);
        return n == null ? 0 : n;
    }

    /** Cantidad de requerimientos por estado (solo los estados que tienen alguno). */
    public Map<Estado, Long> contarPorEstado() {
        Map<Estado, Long> conteo = new EnumMap<>(Estado.class);
        jdbc.query("SELECT ESTADO, COUNT(*) AS CANTIDAD FROM LAB_REQUERIMIENTO GROUP BY ESTADO", Map.of(),
                rs -> {
                    conteo.put(Estado.valueOf(rs.getString("ESTADO")), rs.getLong("CANTIDAD"));
                });
        return conteo;
    }

    /** Suma de HH_TOTAL de los requerimientos APROBADOS. */
    public BigDecimal sumarHhAprobadas() {
        return jdbc.queryForObject("""
                SELECT NVL(SUM(e.HH_TOTAL), 0)
                  FROM LAB_ESTIMACION e
                  JOIN LAB_REQUERIMIENTO r ON r.ID = e.REQUERIMIENTO_ID
                 WHERE r.ESTADO = 'APROBADO'
                """, Map.of(), BigDecimal.class);
    }

    /** Requerimientos por aplicación; incluye las aplicaciones del catálogo sin requerimientos (0). */
    public List<CantidadPorAplicacion> contarPorAplicacion() {
        return jdbc.query("""
                SELECT a.CODIGO, COUNT(r.ID) AS CANTIDAD
                  FROM LAB_APLICACION a
                  LEFT JOIN LAB_REQUERIMIENTO r ON r.APLICACION_ID = a.ID
                 GROUP BY a.CODIGO
                 ORDER BY a.CODIGO
                """, Map.of(), (rs, i) -> new CantidadPorAplicacion(rs.getString("CODIGO"), rs.getLong("CANTIDAD")));
    }

    /**
     * Conteo por aplicación y estado de los requerimientos creados en [inicio, fin) (UTC),
     * opcionalmente de una sola aplicación (por código). null = sin ese filtro.
     */
    public List<FilaSeguimiento> seguimiento(LocalDateTime inicioUtc, LocalDateTime finUtc, String aplicacion) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT a.CODIGO, r.ESTADO, COUNT(*) AS CANTIDAD,
                       NVL(SUM(CASE WHEN r.ESTADO = 'APROBADO' THEN e.HH_TOTAL END), 0) AS HH_APROBADAS
                  FROM LAB_REQUERIMIENTO r
                  JOIN LAB_APLICACION a ON a.ID = r.APLICACION_ID
                  LEFT JOIN LAB_ESTIMACION e ON e.REQUERIMIENTO_ID = r.ID
                 WHERE 1 = 1
                """);
        if (inicioUtc != null) {
            sql.append(" AND r.FECHA_CREACION >= :inicio");
            params.put("inicio", inicioUtc);
        }
        if (finUtc != null) {
            sql.append(" AND r.FECHA_CREACION < :fin");
            params.put("fin", finUtc);
        }
        if (aplicacion != null) {
            sql.append(" AND a.CODIGO = :aplicacion");
            params.put("aplicacion", aplicacion);
        }
        sql.append(" GROUP BY a.CODIGO, r.ESTADO");
        return jdbc.query(sql.toString(), params, (rs, i) -> new FilaSeguimiento(
                rs.getString("CODIGO"),
                Estado.valueOf(rs.getString("ESTADO")),
                rs.getLong("CANTIDAD"),
                rs.getBigDecimal("HH_APROBADAS")));
    }
}
