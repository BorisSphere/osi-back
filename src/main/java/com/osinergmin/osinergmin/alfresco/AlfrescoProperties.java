package com.osinergmin.osinergmin.alfresco;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión con Alfresco, desde variables de entorno (nunca en Git):
 * APP_ALFRESCO_URL (ej. http://localhost:8081/alfresco), APP_ALFRESCO_USUARIO, APP_ALFRESCO_PASSWORD
 * y APP_ALFRESCO_CARPETA (carpeta dentro de "Compartido"; por defecto "Reportes").
 * Si falta la URL o la contraseña, el archivado queda deshabilitado y el resto del sistema funciona igual.
 */
@ConfigurationProperties(prefix = "app.alfresco")
public record AlfrescoProperties(String url, String usuario, String password, String carpeta) {

    public AlfrescoProperties {
        carpeta = carpeta == null || carpeta.isBlank() ? "Reportes" : carpeta.trim();
        url = url == null ? "" : url.trim().replaceAll("/+$", "");
    }

    public boolean configurado() {
        return !url.isBlank() && usuario != null && !usuario.isBlank() && password != null && !password.isBlank();
    }

    /** Base de la API REST pública v1 (disponible desde Alfresco 5.2). */
    public String apiBase() {
        return url + "/api/-default-/public/alfresco/versions/1";
    }
}
