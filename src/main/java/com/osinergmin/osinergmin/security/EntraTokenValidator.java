package com.osinergmin.osinergmin.security;

import com.osinergmin.osinergmin.exception.EntraAccesoDenegadoException;
import com.osinergmin.osinergmin.exception.EntraTokenInvalidoException;
import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Valida los access tokens de Microsoft Entra ID (firma con las claves públicas del tenant, emisor v2.0,
 * audiencia, vencimiento y scope). No se expone como bean JwtDecoder: ese lo usa el JWT propio del backend.
 */
@Component
public class EntraTokenValidator {

    static final String SCOPE_REQUERIDO = "access_as_user";

    private final EntraProperties properties;
    private final NimbusJwtDecoder decoder;

    public EntraTokenValidator(EntraProperties properties) {
        this.properties = properties;
        this.decoder = properties.enabled() ? crearDecoder(properties) : null;
    }

    public Jwt validar(String accessToken) {
        if (decoder == null) {
            throw new EntraAccesoDenegadoException("El inicio de sesión con Microsoft no está habilitado");
        }
        try {
            return decoder.decode(accessToken);
        } catch (JwtException e) {
            throw new EntraTokenInvalidoException("El token de Microsoft no es válido");
        }
    }

    private static NimbusJwtDecoder crearDecoder(EntraProperties properties) {
        String base = "https://login.microsoftonline.com/" + properties.tenantId();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(base + "/discovery/v2.0/keys").build();

        OAuth2TokenValidator<Jwt> audiencia = jwt -> {
            List<String> aud = jwt.getAudience();
            boolean ok = aud != null && (aud.contains(properties.apiClientId())
                    || aud.contains("api://" + properties.apiClientId()));
            return ok ? OAuth2TokenValidatorResult.success() : fallo("Audiencia no válida");
        };
        OAuth2TokenValidator<Jwt> tenant = jwt -> properties.tenantId().equalsIgnoreCase(jwt.getClaimAsString("tid"))
                ? OAuth2TokenValidatorResult.success()
                : fallo("Tenant no válido");
        OAuth2TokenValidator<Jwt> scope = jwt -> {
            String scp = jwt.getClaimAsString("scp");
            boolean ok = scp != null && List.of(scp.split(" ")).contains(SCOPE_REQUERIDO);
            return ok ? OAuth2TokenValidatorResult.success() : fallo("Scope no válido");
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(base + "/v2.0"), audiencia, tenant, scope));
        return decoder;
    }

    private static OAuth2TokenValidatorResult fallo(String descripcion) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", descripcion, null));
    }
}
