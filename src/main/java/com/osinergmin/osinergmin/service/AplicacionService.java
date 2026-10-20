package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.AplicacionRequest;
import com.osinergmin.osinergmin.dto.AplicacionResponse;
import com.osinergmin.osinergmin.exception.AplicacionNoEncontradaException;
import com.osinergmin.osinergmin.exception.AplicacionYaExisteException;
import com.osinergmin.osinergmin.model.Aplicacion;
import com.osinergmin.osinergmin.repository.AplicacionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AplicacionService {

    private final AplicacionRepository repository;

    public AplicacionService(AplicacionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AplicacionResponse> listar() {
        return repository.findAllByOrderByCodigoAsc().stream().map(AplicacionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AplicacionResponse obtener(Long id) {
        return AplicacionResponse.from(buscar(id));
    }

    @Transactional
    public AplicacionResponse crear(AplicacionRequest request) {
        String nombre = request.nombre().trim();
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new AplicacionYaExisteException(nombre);
        }

        Aplicacion app = new Aplicacion();
        // Si dos altas coinciden, la restricción UNIQUE de CODIGO rechaza la segunda (409).
        app.setCodigo(String.format("LAB-%03d", repository.maxCorrelativo() + 1));
        aplicarCambios(app, request);
        return AplicacionResponse.from(repository.saveAndFlush(app));
    }

    @Transactional
    public AplicacionResponse actualizar(Long id, AplicacionRequest request) {
        Aplicacion app = buscar(id);
        String nombre = request.nombre().trim();
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new AplicacionYaExisteException(nombre);
        }

        aplicarCambios(app, request);
        return AplicacionResponse.from(repository.saveAndFlush(app));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
        // Forzar el DELETE aquí: si la aplicación está referenciada, el error sale como 409 y no al hacer commit.
        repository.flush();
    }

    private Aplicacion buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new AplicacionNoEncontradaException(id));
    }

    private static void aplicarCambios(Aplicacion app, AplicacionRequest request) {
        app.setNombre(request.nombre().trim());
        app.setDescripcion(request.descripcion().trim());
        app.setTecnologiaActual(textoOpcional(request.tecnologiaActual()));
        app.setTecnologiaObjetivo(textoOpcional(request.tecnologiaObjetivo()));
        app.setActivo(request.activo());
    }

    /** Vacío o solo espacios se guarda como NULL. */
    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
