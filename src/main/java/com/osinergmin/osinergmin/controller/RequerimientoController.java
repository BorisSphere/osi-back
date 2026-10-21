package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.dto.RequerimientoRequest;
import com.osinergmin.osinergmin.dto.RequerimientoResponse;
import com.osinergmin.osinergmin.model.EstadoRequerimiento;
import com.osinergmin.osinergmin.security.JwtService;
import com.osinergmin.osinergmin.service.RequerimientoService;
import com.osinergmin.osinergmin.service.RequerimientoService.ArchivoDescarga;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Requerimientos: todos consultan; crear, modificar y eliminar exigen su permiso (ver Rol). */
@RestController
@RequestMapping("/api/requerimientos")
public class RequerimientoController {

    private final RequerimientoService requerimientoService;

    public RequerimientoController(RequerimientoService requerimientoService) {
        this.requerimientoService = requerimientoService;
    }

    /** El parámetro estado es opcional; vacío equivale a no filtrar. */
    @GetMapping
    public List<RequerimientoResponse> listar(@RequestParam(required = false) EstadoRequerimiento estado) {
        return requerimientoService.listar(estado);
    }

    @GetMapping("/{id}")
    public RequerimientoResponse obtener(@PathVariable Long id) {
        return requerimientoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REQUERIMIENTO_CREAR')")
    public ResponseEntity<RequerimientoResponse> crear(@Valid @RequestBody RequerimientoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Number uid = jwt.getClaim(JwtService.CLAIM_USER_ID);
        RequerimientoResponse creado = requerimientoService.crear(request, uid.longValue());
        return ResponseEntity.created(URI.create("/api/requerimientos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REQUERIMIENTO_MODIFICAR')")
    public RequerimientoResponse actualizar(@PathVariable Long id, @Valid @RequestBody RequerimientoRequest request) {
        return requerimientoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REQUERIMIENTO_ELIMINAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        requerimientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** multipart/form-data con el campo "archivo". Reemplaza el adjunto anterior si existía. */
    @PostMapping(path = "/{id}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('REQUERIMIENTO_MODIFICAR')")
    public RequerimientoResponse adjuntarArchivo(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        return requerimientoService.adjuntarArchivo(id, archivo);
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable Long id) {
        ArchivoDescarga descarga = requerimientoService.descargarArchivo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(descarga.nombre(), StandardCharsets.UTF_8).build()
                                .toString())
                .body(descarga.recurso());
    }
}
