package com.osinergmin.osinergmin.dto;

import com.osinergmin.osinergmin.model.Rol;
import com.osinergmin.osinergmin.model.Usuario;

/** Datos públicos del usuario: nunca incluye el hash de la contraseña. */
public record UsuarioResponse(Long id, String username, Rol rol, boolean activo) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getRol(), usuario.getActivo());
    }
}
