package com.osinergmin.osinergmin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estimación de horas de un requerimiento (regla 5 / 78 / 15 / 2).
 * UQ_LAB_EST_REQ: una sola por requerimiento; al reestimar se actualiza la fila existente.
 */
@Entity
@Table(name = "LAB_ESTIMACION")
@Getter
@Setter
@NoArgsConstructor
public class Estimacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** FK a LAB_REQUERIMIENTO; se guarda solo el ID para no depender de la entidad de requerimientos. */
    @Column(name = "REQUERIMIENTO_ID", nullable = false, unique = true)
    private Long requerimientoId;

    @Column(name = "HH_DESARROLLO", nullable = false, precision = 12, scale = 2)
    private BigDecimal hhDesarrollo;

    @Column(name = "HH_ANALISIS", nullable = false, precision = 12, scale = 2)
    private BigDecimal hhAnalisis;

    @Column(name = "HH_PRUEBAS", nullable = false, precision = 12, scale = 2)
    private BigDecimal hhPruebas;

    @Column(name = "HH_DOCUMENTACION", nullable = false, precision = 12, scale = 2)
    private BigDecimal hhDocumentacion;

    @Column(name = "HH_TOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal hhTotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ESTIMADO_POR", nullable = false)
    private Usuario estimadoPor;

    @Column(name = "FECHA_ESTIMACION", nullable = false)
    private Instant fechaEstimacion;
}
