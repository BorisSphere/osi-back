package com.osinergmin.osinergmin.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EstimacionRequest(
        @NotNull(message = "Las horas de desarrollo son obligatorias")
        @DecimalMin(value = "0", inclusive = false, message = "Las horas de desarrollo deben ser mayores a cero")
        @DecimalMax(value = "10000", message = "Las horas de desarrollo no pueden superar 10000")
        @Digits(integer = 5, fraction = 2, message = "Use como máximo 2 decimales")
        BigDecimal hhDesarrollo) {
}
