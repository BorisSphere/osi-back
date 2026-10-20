package com.osinergmin.osinergmin.dto;

import com.osinergmin.osinergmin.model.Aplicacion;

public record AplicacionResponse(Long id, String codigo, String nombre, String descripcion, String tecnologiaActual,
        String tecnologiaObjetivo, boolean activo) {

    public static AplicacionResponse from(Aplicacion app) {
        return new AplicacionResponse(app.getId(), app.getCodigo(), app.getNombre(), app.getDescripcion(),
                app.getTecnologiaActual(), app.getTecnologiaObjetivo(), app.getActivo());
    }
}
