package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.dto.AplicacionRequest;
import com.osinergmin.osinergmin.dto.AplicacionResponse;
import com.osinergmin.osinergmin.service.AplicacionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de aplicaciones: todos los roles lo consultan, solo COORDINADOR lo modifica. */
@RestController
@RequestMapping("/api/aplicaciones")
public class AplicacionController {

    private final AplicacionService aplicacionService;

    public AplicacionController(AplicacionService aplicacionService) {
        this.aplicacionService = aplicacionService;
    }

    @GetMapping
    public List<AplicacionResponse> listar() {
        return aplicacionService.listar();
    }

    @GetMapping("/{id}")
    public AplicacionResponse obtener(@PathVariable Long id) {
        return aplicacionService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINADOR')")
    public ResponseEntity<AplicacionResponse> crear(@Valid @RequestBody AplicacionRequest request) {
        AplicacionResponse creada = aplicacionService.crear(request);
        return ResponseEntity.created(URI.create("/api/aplicaciones/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public AplicacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody AplicacionRequest request) {
        return aplicacionService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        aplicacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
