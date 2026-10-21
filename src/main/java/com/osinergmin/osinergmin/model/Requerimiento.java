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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "LAB_REQUERIMIENTO")
@Getter
@Setter
@NoArgsConstructor
public class Requerimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APLICACION_ID", nullable = false)
    private Aplicacion aplicacion;

    @Column(name = "TITULO", nullable = false, length = 180)
    private String titulo;

    @Column(name = "DESCRIPCION", nullable = false, length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRIORIDAD", nullable = false, length = 12)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoRequerimiento estado = EstadoRequerimiento.REGISTRADO;

    @Column(name = "TIPO", length = 50)
    private String tipo;

    /** Fecha requerida por el solicitante (columna FECHA, tipo DATE). */
    @Column(name = "FECHA")
    private LocalDate fechaRequerida;

    @Column(name = "DETALLE_TECNICO", length = 2000)
    private String detalleTecnico;

    /** Nombre original del archivo adjunto. */
    @Column(name = "ARCHIVO_NOMBRE", length = 255)
    private String archivoNombre;

    /** Ubicación relativa dentro de la carpeta de almacenamiento del servidor (no es una ruta de la PC del usuario). */
    @Column(name = "ARCHIVO_RUTA", length = 1000)
    private String archivoRuta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CREADO_POR", nullable = false, updatable = false)
    private Usuario creadoPor;

    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "FECHA_ACTUALIZACION", nullable = false)
    private Instant fechaActualizacion;

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = Instant.now();
    }
}
