package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.RequerimientoRequest;
import com.osinergmin.osinergmin.dto.RequerimientoResponse;
import com.osinergmin.osinergmin.exception.AplicacionNoEncontradaException;
import com.osinergmin.osinergmin.exception.ArchivoNoEncontradoException;
import com.osinergmin.osinergmin.exception.RequerimientoNoEditableException;
import com.osinergmin.osinergmin.exception.RequerimientoNoEncontradoException;
import com.osinergmin.osinergmin.model.EstadoRequerimiento;
import com.osinergmin.osinergmin.model.Requerimiento;
import com.osinergmin.osinergmin.model.Usuario;
import com.osinergmin.osinergmin.repository.AplicacionRepository;
import com.osinergmin.osinergmin.repository.RequerimientoRepository;
import com.osinergmin.osinergmin.repository.UsuarioRepository;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RequerimientoService {

    private final RequerimientoRepository repository;
    private final AplicacionRepository aplicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArchivoStorageService archivoStorage;
    private final CorreoRequerimientoPublisher correoPublisher;

    public RequerimientoService(RequerimientoRepository repository, AplicacionRepository aplicacionRepository,
            UsuarioRepository usuarioRepository, ArchivoStorageService archivoStorage,
            CorreoRequerimientoPublisher correoPublisher) {
        this.repository = repository;
        this.aplicacionRepository = aplicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.archivoStorage = archivoStorage;
        this.correoPublisher = correoPublisher;
    }

    /** Sin estado devuelve todos los requerimientos. */
    @Transactional(readOnly = true)
    public List<RequerimientoResponse> listar(EstadoRequerimiento estado) {
        List<Requerimiento> lista = estado == null
                ? repository.findAllByOrderByFechaCreacionDescIdDesc()
                : repository.findAllByEstadoOrderByFechaCreacionDescIdDesc(estado);
        return lista.stream().map(RequerimientoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RequerimientoResponse obtener(Long id) {
        return RequerimientoResponse.from(buscar(id));
    }

    @Transactional
    public RequerimientoResponse crear(RequerimientoRequest request, Long usuarioId) {
        Usuario creador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("El usuario autenticado ya no existe: " + usuarioId));

        Requerimiento requerimiento = new Requerimiento();
        requerimiento.setCreadoPor(creador);
        requerimiento.setEstado(EstadoRequerimiento.REGISTRADO);
        aplicarCambios(requerimiento, request);
        Requerimiento guardado = repository.saveAndFlush(requerimiento);
        correoPublisher.registrado(guardado);
        return RequerimientoResponse.from(guardado);
    }

    /** Corrige los datos de un requerimiento; el estado no se toca (lo cambian estimación y aprobación). */
    @Transactional
    public RequerimientoResponse actualizar(Long id, RequerimientoRequest request) {
        Requerimiento requerimiento = buscar(id);
        if (requerimiento.getEstado() == EstadoRequerimiento.APROBADO) {
            throw new RequerimientoNoEditableException(id);
        }

        aplicarCambios(requerimiento, request);
        return RequerimientoResponse.from(repository.saveAndFlush(requerimiento));
    }

    @Transactional
    public void eliminar(Long id) {
        Requerimiento requerimiento = buscar(id);
        String ruta = requerimiento.getArchivoRuta();
        repository.delete(requerimiento);
        // Forzar el DELETE aquí: si otra tabla lo referencia, el error sale como 409 y no al hacer commit.
        repository.flush();
        archivoStorage.eliminar(ruta);
    }

    /** Adjunta (o reemplaza) el archivo del requerimiento. */
    @Transactional
    public RequerimientoResponse adjuntarArchivo(Long id, MultipartFile archivo) {
        Requerimiento requerimiento = buscar(id);
        if (requerimiento.getEstado() == EstadoRequerimiento.APROBADO) {
            throw new RequerimientoNoEditableException(id);
        }

        String anterior = requerimiento.getArchivoRuta();
        String nombre = archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
        requerimiento.setArchivoRuta(archivoStorage.guardar(id, archivo));
        requerimiento.setArchivoNombre(nombre.length() > 255 ? nombre.substring(nombre.length() - 255) : nombre);
        RequerimientoResponse respuesta = RequerimientoResponse.from(repository.saveAndFlush(requerimiento));
        archivoStorage.eliminar(anterior);
        return respuesta;
    }

    @Transactional(readOnly = true)
    public ArchivoDescarga descargarArchivo(Long id) {
        Requerimiento requerimiento = buscar(id);
        if (requerimiento.getArchivoRuta() == null) {
            throw new ArchivoNoEncontradoException(id);
        }
        Resource recurso = archivoStorage.cargar(requerimiento.getArchivoRuta());
        if (recurso == null) {
            throw new ArchivoNoEncontradoException(id);
        }
        return new ArchivoDescarga(requerimiento.getArchivoNombre(), recurso);
    }

    public record ArchivoDescarga(String nombre, Resource recurso) {
    }


    private Requerimiento buscar(Long id) {
        return repository.findWithDetalleById(id).orElseThrow(() -> new RequerimientoNoEncontradoException(id));
    }

    private void aplicarCambios(Requerimiento requerimiento, RequerimientoRequest request) {
        requerimiento.setAplicacion(aplicacionRepository.findById(request.aplicacionId())
                .orElseThrow(() -> new AplicacionNoEncontradaException(request.aplicacionId())));
        requerimiento.setTitulo(request.titulo().trim());
        requerimiento.setDescripcion(request.descripcion().trim());
        requerimiento.setPrioridad(request.prioridad());
        requerimiento.setTipo(textoOpcional(request.tipo()));
        requerimiento.setFechaRequerida(request.fechaRequerida());
        requerimiento.setDetalleTecnico(textoOpcional(request.detalleTecnico()));
    }

    /** Vacío o solo espacios se guarda como NULL. */
    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
