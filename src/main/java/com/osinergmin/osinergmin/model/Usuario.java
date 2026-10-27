package com.osinergmin.osinergmin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "LAB_USUARIO")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME", nullable = false, length = 80, unique = true)
    private String username;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 120)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROL", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "CORREO", length = 254, unique = true)
    private String correo;

    @Column(name = "ENTRA_OID", length = 36, unique = true)
    private String entraOid;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;
}
