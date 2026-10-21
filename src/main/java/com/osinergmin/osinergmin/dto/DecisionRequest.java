package com.osinergmin.osinergmin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de PATCH /api/requerimientos/{id}/decision. El motivo es obligatorio solo al rechazar. */
public record DecisionRequest(
        @NotNull(message = "La decisión es obligatoria (APROBADO o RECHAZADO)")
        Decision decision,

        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo) {

    public enum Decision {
        APROBADO,
        RECHAZADO
    }
}
