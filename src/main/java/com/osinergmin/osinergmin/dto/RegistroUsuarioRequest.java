package com.osinergmin.osinergmin.dto;

import com.osinergmin.osinergmin.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroUsuarioRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 3, max = 80, message = "El usuario debe tener entre 3 y 80 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El usuario solo puede contener letras, números, punto, guion y guion bajo")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        // BCrypt solo usa los primeros 72 bytes de la contraseña.
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio (ANALISTA, COORDINADOR o APROBADOR)")
        Rol rol) {
}
