package com.osinergmin.osinergmin.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del refresh token (prefijo {@code app.refresh}).
 *
 * @param expiration   tiempo de vida del refresh token, p. ej. {@code 7d}
 * @param reuseGrace   margen en que un token recién rotado aún se acepta (varias pestañas
 *                     refrescando a la vez); pasado ese margen, reusarlo se trata como robo
 * @param cookieName   nombre de la cookie HttpOnly
 * @param cookieSecure enviar la cookie solo por HTTPS (desactivar únicamente en desarrollo con Safari)
 */
@ConfigurationProperties(prefix = "app.refresh")
public record RefreshTokenProperties(Duration expiration, Duration reuseGrace, String cookieName,
        boolean cookieSecure) {
}
