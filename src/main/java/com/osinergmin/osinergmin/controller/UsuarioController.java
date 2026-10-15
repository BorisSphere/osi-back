package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.model.Usuario;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @GetMapping
    public Usuario obtenerUsuario() {
        return new Usuario("Joselin", 31);
    }

    @PostMapping
    public Usuario crearUsuario(@RequestBody Usuario usuario) {
        return usuario;
    }
}