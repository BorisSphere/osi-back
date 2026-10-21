package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.dto.RegistroUsuarioRequest;
import com.osinergmin.osinergmin.dto.UsuarioResponse;
import com.osinergmin.osinergmin.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de usuarios: permiso USUARIO_ADMINISTRAR (también protegido en SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasAuthority('USUARIO_ADMINISTRAR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroUsuarioRequest request) {
        UsuarioResponse creado = usuarioService.registrar(request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.id())).body(creado);
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }
}
