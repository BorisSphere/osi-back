package com.osinergmin.osinergmin.dto;

import com.osinergmin.osinergmin.model.Prioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Datos para registrar o corregir un requerimiento. El estado, el creador y las fechas de control los asigna el backend. */
public record RequerimientoRequest(
        @NotNull(message = "La aplicación es obligatoria")
        Long aplicacionId,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 180, message = "El título no puede superar los 180 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
        String descripcion,

        @NotNull(message = "La prioridad es obligatoria")
        Prioridad prioridad,

        @Size(max = 50, message = "El tipo no puede superar los 50 caracteres")
        String tipo,

        LocalDate fechaRequerida,

        @Size(max = 2000, message = "El detalle técnico no puede superar los 2000 caracteres")
        String detalleTecnico) {
}
