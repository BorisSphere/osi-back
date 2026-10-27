package com.osinergmin.osinergmin.dto;

import jakarta.validation.constraints.NotBlank;

/** Access token emitido por Microsoft Entra ID para esta API (scope access_as_user). */
public record EntraLoginRequest(@NotBlank(message = "El token es obligatorio") String accessToken) {
}
