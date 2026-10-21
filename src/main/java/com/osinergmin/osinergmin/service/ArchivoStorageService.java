package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.exception.ArchivoInvalidoException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Guarda los adjuntos en una carpeta del servidor; la BD solo conserva la ubicación relativa a esa carpeta. */
@Service
public class ArchivoStorageService {

    private final Path raiz;

    public ArchivoStorageService(@Value("${app.storage.dir:C:/osi-uploads}") String directorio) {
        this.raiz = Path.of(directorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de adjuntos: " + raiz, e);
        }
    }

    /** Devuelve la ruta relativa guardada: {requerimientoId}/{uuid}_{nombre}. */
    public String guardar(Long requerimientoId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("El archivo está vacío");
        }
        String relativa = requerimientoId + "/" + UUID.randomUUID() + "_" + nombreSeguro(archivo.getOriginalFilename());
        Path destino = resolver(relativa);
        try {
            Files.createDirectories(destino.getParent());
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
        return relativa;
    }

    /** Devuelve null si el archivo ya no existe en disco. */
    public Resource cargar(String rutaRelativa) {
        Path archivo = resolver(rutaRelativa);
        return Files.isReadable(archivo) ? new PathResource(archivo) : null;
    }

    public void eliminar(String rutaRelativa) {
        if (rutaRelativa == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolver(rutaRelativa));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar el archivo", e);
        }
    }

    /** Impide salir de la carpeta de almacenamiento (path traversal). */
    private Path resolver(String rutaRelativa) {
        Path resuelta = raiz.resolve(rutaRelativa).normalize();
        if (!resuelta.startsWith(raiz)) {
            throw new ArchivoInvalidoException("Ruta de archivo no válida");
        }
        return resuelta;
    }

    private static String nombreSeguro(String original) {
        String nombre = original == null ? "" : Path.of(original.replace('\\', '/')).getFileName().toString();
        nombre = nombre.replaceAll("[^A-Za-z0-9._-]", "_");
        if (nombre.isBlank()) {
            throw new ArchivoInvalidoException("El archivo no tiene nombre");
        }
        return nombre.length() > 150 ? nombre.substring(nombre.length() - 150) : nombre;
    }
}
