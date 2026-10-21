package com.osinergmin.osinergmin.dto;

/** Resultado de la decisión. motivoRechazo es null al aprobar. */
public record DecisionResponse(long requerimientoId, String codigo, String estado, String motivoRechazo) {
}
