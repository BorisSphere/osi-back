package com.osinergmin.osinergmin.alfresco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Partes de las respuestas JSON de la API de Alfresco que usa el sistema (se ignora el resto). */
public final class AlfrescoNodo {

    /** Alfresco devuelve fechas como 2026-10-27T21:34:07.210+0000 (offset sin dos puntos). */
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private AlfrescoNodo() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Nodo(
            String id,
            String name,
            Boolean isFolder,
            Boolean isFile,
            String createdAt,
            Contenido content,
            Map<String, Object> properties,
            Ruta path) {

        public Instant creado() {
            return createdAt == null ? null : OffsetDateTime.parse(createdAt, FECHA).toInstant();
        }

        public String propiedad(String nombre) {
            Object valor = properties == null ? null : properties.get(nombre);
            return valor == null ? null : valor.toString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contenido(String mimeType, Long sizeInBytes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ruta(String name, List<Elemento> elements) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Elemento(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entrada(Nodo entry) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Lista(Pagina list) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagina(List<Entrada> entries) {}
}
