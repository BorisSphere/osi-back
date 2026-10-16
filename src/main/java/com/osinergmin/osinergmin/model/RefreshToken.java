package com.osinergmin.osinergmin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "LAB_REFRESH_TOKEN")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private Usuario usuario;

    /** SHA-256 en hexadecimal del token entregado al navegador. */
    @Column(name = "TOKEN_HASH", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "FECHA_CREACION", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "FECHA_EXPIRACION", nullable = false)
    private Instant fechaExpiracion;

    /** Momento en que se rotó por uno nuevo; null si aún no se usó. */
    @Column(name = "FECHA_USO")
    private Instant fechaUso;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "REVOCADO", nullable = false)
    private Boolean revocado = false;
}
