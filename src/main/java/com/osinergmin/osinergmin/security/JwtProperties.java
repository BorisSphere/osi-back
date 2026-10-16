package com.osinergmin.osinergmin.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del JWT (prefijo {@code app.jwt}).
 *
 * @param secret     clave HMAC-SHA256; mínimo 32 caracteres
 * @param expiration tiempo de vida del token, p. ej. {@code 8h}
 * @param issuer     emisor que se firma en el token y se valida al recibirlo
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres");
        }
    }
}
