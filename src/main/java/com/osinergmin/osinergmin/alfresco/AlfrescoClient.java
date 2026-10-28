package com.osinergmin.osinergmin.alfresco;

import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Entrada;
import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Lista;
import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Nodo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente mínimo de la API REST pública v1 de Alfresco: carpetas, subida, listado y descarga.
 * Toda la comunicación con Alfresco pasa por aquí (el front nunca habla con Alfresco).
 */
public class AlfrescoClient {

    private static final Logger log = LoggerFactory.getLogger(AlfrescoClient.class);
    /** Alias de la API para la carpeta "Compartido" (Shared). */
    private static final String COMPARTIDO = "-shared-";

    private final AlfrescoProperties properties;
    private final RestClient rest;

    public AlfrescoClient(AlfrescoProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.rest = builder
                .baseUrl(properties.apiBase())
                .defaultHeaders(h -> {
                    if (properties.configurado()) {
                        h.setBasicAuth(properties.usuario(), properties.password());
                    }
                })
                .build();
    }

    public boolean configurado() {
        return properties.configurado();
    }

    /** ID de la carpeta raíz de reportes (Compartido/<carpeta>). Debe existir: se crea a mano en Alfresco. */
    public String carpetaReportes() {
        return idPorRuta(COMPARTIDO, properties.carpeta()).orElseThrow(() -> new AlfrescoNoDisponibleException(
                "No existe la carpeta \"" + properties.carpeta() + "\" dentro de Compartido en Alfresco"));
    }

    /** Devuelve el ID de la subcarpeta {padre}/{nombre}, creándola si no existe. */
    public String obtenerOCrearCarpeta(String padreId, String nombre) {
        Optional<String> existente = idPorRuta(padreId, nombre);
        if (existente.isPresent()) {
            return existente.get();
        }
        try {
            return llamar(() -> rest.post()
                    .uri("/nodes/{id}/children", padreId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("name", nombre, "nodeType", "cm:folder"))
                    .retrieve()
                    .body(Entrada.class)).entry().id();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                // Otra petición la creó al mismo tiempo: se usa esa.
                return idPorRuta(padreId, nombre).orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    /** Sube un archivo a la carpeta; Alfresco le agrega un sufijo si el nombre ya existe. */
    public Nodo subir(String carpetaId, String nombre, byte[] contenido, MediaType tipo, String titulo, String descripcion) {
        // Formulario multipart clásico (MultipartBodyBuilder exige las librerías reactivas, que no se usan).
        HttpHeaders cabeceraArchivo = new HttpHeaders();
        cabeceraArchivo.setContentType(tipo);
        cabeceraArchivo.setContentDisposition(ContentDisposition.formData().name("filedata").filename(nombre).build());
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add("filedata", new HttpEntity<>(new ByteArrayResource(contenido), cabeceraArchivo));
        partes.add("name", nombre);
        partes.add("nodeType", "cm:content");
        partes.add("autoRename", "true");
        // Campos extra del formulario = propiedades del documento.
        partes.add("cm:title", titulo);
        partes.add("cm:description", descripcion);
        return llamar(() -> rest.post()
                .uri("/nodes/{id}/children", carpetaId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(partes)
                .retrieve()
                .body(Entrada.class)).entry();
    }

    /** Hijos de una carpeta: solo carpetas o solo archivos, con sus propiedades. */
    public List<Nodo> hijos(String carpetaId, boolean carpetas) {
        Lista lista = llamar(() -> rest.get()
                .uri("/nodes/{id}/children?where={filtro}&include=properties&maxItems=500",
                        carpetaId, carpetas ? "(isFolder=true)" : "(isFile=true)")
                .retrieve()
                .body(Lista.class));
        List<Nodo> nodos = new ArrayList<>();
        if (lista != null && lista.list() != null && lista.list().entries() != null) {
            lista.list().entries().forEach(e -> nodos.add(e.entry()));
        }
        return nodos;
    }

    /** Datos de un nodo con su ruta completa (para validar dónde está). Vacío si no existe. */
    public Optional<Nodo> nodoConRuta(String id) {
        try {
            return Optional.ofNullable(llamar(() -> rest.get()
                    .uri("/nodes/{id}?include=path,properties", id)
                    .retrieve()
                    .body(Entrada.class)).entry());
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    public byte[] contenido(String id) {
        return llamar(() -> rest.get().uri("/nodes/{id}/content", id).retrieve().body(byte[].class));
    }

    private Optional<String> idPorRuta(String baseId, String rutaRelativa) {
        try {
            Entrada entrada = llamar(() -> rest.get()
                    .uri("/nodes/{id}?relativePath={ruta}", baseId, rutaRelativa)
                    .retrieve()
                    .body(Entrada.class));
            return Optional.of(entrada.entry().id());
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    /**
     * Ejecuta una llamada traduciendo los problemas de conexión y de credenciales a
     * AlfrescoNoDisponibleException (503). Los demás errores HTTP siguen su curso.
     */
    private <T> T llamar(Supplier<T> llamada) {
        if (!properties.configurado()) {
            throw new AlfrescoNoDisponibleException(
                    "Alfresco no está configurado (faltan APP_ALFRESCO_URL, APP_ALFRESCO_USUARIO o APP_ALFRESCO_PASSWORD)");
        }
        try {
            return llamada.get();
        } catch (ResourceAccessException ex) {
            log.warn("Alfresco no responde en {}: {}", properties.url(), ex.getMessage());
            throw new AlfrescoNoDisponibleException("Alfresco no responde. Verifique que esté en ejecución.", ex);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401 || status == 403) {
                log.warn("Alfresco rechazó las credenciales del usuario {} ({})", properties.usuario(), status);
                throw new AlfrescoNoDisponibleException("Alfresco rechazó las credenciales configuradas.", ex);
            }
            throw ex;
        }
    }
}
