package com.osinergmin.osinergmin.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Usuario técnico con el que Prometheus lee {@code /actuator/prometheus} por HTTP Basic (prefijo {@code app.metrics}).
 * Si falta el usuario o la contraseña no se crea ninguno y el endpoint responde siempre 401.
 *
 * @param username usuario de Basic auth
 * @param password contraseña de Basic auth
 */
@ConfigurationProperties(prefix = "app.metrics")
public record MetricsProperties(String username, String password) {

    public boolean habilitado() {
        return username != null && !username.isBlank() && password != null && !password.isBlank();
    }
}
