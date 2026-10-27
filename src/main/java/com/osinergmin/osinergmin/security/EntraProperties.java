package com.osinergmin.osinergmin.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Login con Microsoft Entra ID (prefijo {@code app.entra}).
 *
 * @param enabled     habilita el endpoint /api/auth/entra
 * @param tenantId    Directory (tenant) ID
 * @param apiClientId Application (client) ID de la app registrada para este backend (audiencia del token)
 * @param autoCrear   crea el usuario local en el primer ingreso si no existe
 */
@ConfigurationProperties(prefix = "app.entra")
public record EntraProperties(boolean enabled, String tenantId, String apiClientId, boolean autoCrear) {

    public EntraProperties {
        if (enabled && (isBlank(tenantId) || isBlank(apiClientId))) {
            throw new IllegalStateException("app.entra.tenant-id y app.entra.api-client-id son obligatorios");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
