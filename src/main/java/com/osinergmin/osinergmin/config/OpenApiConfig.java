package com.osinergmin.osinergmin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Documentación de la API en /swagger-ui.html. El botón "Authorize" pide el access token
 * que devuelve POST /api/auth/login y lo envía como "Authorization: Bearer ...".
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";
    private static final Pattern AUTHORITY = Pattern.compile("hasAuthority\\('([A-Z_]+)'\\)");

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("osi-back — Lab Modernización")
                        .version("v1")
                        .description("""
                                API de gestión de requerimientos de modernización tecnológica.

                                **Cómo autenticarse:** ejecuta `POST /api/auth/login`, copia el `token` de la \
                                respuesta y pégalo en **Authorize**. El token dura 15 minutos.

                                Cada endpoint indica el permiso que exige; sin él responde 403."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                // Todos los endpoints piden token salvo login/refresh/logout (@SecurityRequirements vacío).
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /** Agrega a cada endpoint el permiso que exige su @PreAuthorize (del método o de la clase). */
    @Bean
    public OperationCustomizer permisoEnDescripcion() {
        return (operation, handlerMethod) -> {
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (preAuthorize == null) {
                preAuthorize = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
            }
            if (preAuthorize == null) {
                return operation;
            }
            Matcher matcher = AUTHORITY.matcher(preAuthorize.value());
            String permiso = matcher.find() ? "`" + matcher.group(1) + "`" : "`" + preAuthorize.value() + "`";
            String descripcion = operation.getDescription() == null ? "" : operation.getDescription() + "\n\n";
            return operation.description(descripcion + "**Permiso requerido:** " + permiso);
        };
    }
}
