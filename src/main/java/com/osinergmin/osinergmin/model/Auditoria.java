package com.osinergmin.osinergmin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Evento auditable sobre un requerimiento (crear, estimar, aprobar, rechazar). */
@Entity
@Table(name = "LAB_AUDITORIA")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** FK a LAB_REQUERIMIENTO; se guarda solo el ID para no depender de la entidad de requerimientos. */
    @Column(name = "REQUERIMIENTO_ID", nullable = false)
    private Long requerimientoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ACTOR_ID", nullable = false)
    private Usuario actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCION", nullable = false, length = 40)
    private AccionAuditoria accion;

    /** Descripción legible; en un RECHAZO contiene el motivo. */
    @Column(name = "DETALLE", length = 1000)
    private String detalle;

    @Column(name = "FECHA", nullable = false)
    private Instant fecha;
}
