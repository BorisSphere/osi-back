package com.osinergmin.osinergmin.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Conversión de fechas de los filtros. El usuario elige días en hora de Perú; las columnas
 * TIMESTAMP guardan instantes en UTC (ver hibernate.jdbc.time_zone en application.properties).
 */
public final class FechasLab {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private FechasLab() {
    }

    /** Primer instante del día en Perú. */
    public static Instant inicioDelDia(LocalDate dia) {
        return dia.atStartOfDay(ZONA).toInstant();
    }

    /** Primer instante del día siguiente (límite exclusivo para "hasta"). */
    public static Instant finDelDia(LocalDate dia) {
        return dia.plusDays(1).atStartOfDay(ZONA).toInstant();
    }

    /** Instante como LocalDateTime UTC, para comparar con una columna TIMESTAMP en SQL directo. */
    public static LocalDateTime utc(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    /** Día en Perú de un TIMESTAMP guardado en UTC. */
    public static LocalDate diaEnPeru(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZONA).toLocalDate();
    }
}
