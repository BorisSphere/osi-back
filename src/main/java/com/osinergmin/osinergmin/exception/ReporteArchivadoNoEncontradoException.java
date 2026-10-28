package com.osinergmin.osinergmin.exception;

/** El documento no existe o no pertenece a la carpeta de reportes → 404. */
public class ReporteArchivadoNoEncontradoException extends RuntimeException {

    public ReporteArchivadoNoEncontradoException(String id) {
        super("No se encontró el reporte archivado " + id);
    }
}
