package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.dto.EstimacionRequest;
import com.osinergmin.osinergmin.dto.EstimacionResponse;
import com.osinergmin.osinergmin.security.JwtService;
import com.osinergmin.osinergmin.service.EstimacionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Estimación de horas (RF05). Según la matriz de permisos, solo el ANALISTA guarda estimaciones. */
@RestController
@RequestMapping("/api/requerimientos")
public class EstimacionController {

    private final EstimacionService estimacionService;

    public EstimacionController(EstimacionService estimacionService) {
        this.estimacionService = estimacionService;
    }

    @PutMapping("/{id}/estimacion")
    @PreAuthorize("hasAuthority('ESTIMACION_GUARDAR')")
    public EstimacionResponse estimar(
            @PathVariable long id,
            @Valid @RequestBody EstimacionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return estimacionService.estimar(id, request.hhDesarrollo(), usuarioId(jwt));
    }

    private static long usuarioId(Jwt jwt) {
        Object uid = jwt.getClaim(JwtService.CLAIM_USER_ID);
        return ((Number) uid).longValue();
    }
}
