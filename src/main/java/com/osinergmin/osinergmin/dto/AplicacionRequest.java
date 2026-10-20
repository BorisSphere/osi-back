package com.osinergmin.osinergmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear o editar una aplicación. El código lo genera el backend. */
public record AplicacionRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 160, message = "El nombre no puede superar los 160 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @Size(max = 160, message = "La tecnología actual no puede superar los 160 caracteres")
        String tecnologiaActual,

        @Size(max = 160, message = "La tecnología objetivo no puede superar los 160 caracteres")
        String tecnologiaObjetivo,

        @NotNull(message = "El estado es obligatorio")
        Boolean activo) {
}
