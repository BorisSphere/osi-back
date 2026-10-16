package com.osinergmin.osinergmin.repository;

import com.osinergmin.osinergmin.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revocado = true where t.usuario.id = :usuarioId and t.revocado = false")
    int revocarTodosDelUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("delete from RefreshToken t where t.fechaExpiracion < :limite")
    int eliminarExpiradosAntesDe(@Param("limite") Instant limite);
}
