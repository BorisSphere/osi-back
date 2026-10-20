package com.osinergmin.osinergmin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "LAB_APLICACION")
@Getter
@Setter
@NoArgsConstructor
public class Aplicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** Código correlativo LAB-001, LAB-002...: lo asigna el backend y no cambia. */
    @Column(name = "CODIGO", nullable = false, length = 30, unique = true, updatable = false)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, length = 160)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    @Column(name = "TECNOLOGIA_ACTUAL", length = 160)
    private String tecnologiaActual;

    @Column(name = "TECNOLOGIA_OBJETIVO", length = 160)
    private String tecnologiaObjetivo;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;
}
