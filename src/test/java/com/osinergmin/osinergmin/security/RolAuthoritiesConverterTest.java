package com.osinergmin.osinergmin.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class RolAuthoritiesConverterTest {

    private final RolAuthoritiesConverter converter = new RolAuthoritiesConverter();

    @Test
    void agregaRolYPermisosDelRol() {
        Set<String> authorities = authorities("APROBADOR");
        assertTrue(authorities.containsAll(Set.of("ROLE_APROBADOR", "REQUERIMIENTO_APROBAR", "AUDITORIA_VER")));
        assertTrue(!authorities.contains("ESTIMACION_GUARDAR"));
    }

    @Test
    void rolDesconocidoNoTienePermisos() {
        assertTrue(authorities("ADMIN").isEmpty());
    }

    private Set<String> authorities(String rol) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("u").claim(JwtService.CLAIM_ROL, rol)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return converter.convert(jwt).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
