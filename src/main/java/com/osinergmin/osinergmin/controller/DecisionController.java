package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.dto.DecisionRequest;
import com.osinergmin.osinergmin.dto.DecisionResponse;
import com.osinergmin.osinergmin.security.JwtService;
import com.osinergmin.osinergmin.service.DecisionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aprobación o rechazo (RF06). Solo el APROBADOR decide; cualquier otro rol recibe 403 (CA02). */
@RestController
@RequestMapping("/api/requerimientos")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasRole('APROBADOR')")
    public DecisionResponse decidir(
            @PathVariable long id,
            @Valid @RequestBody DecisionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        long aprobadorId = ((Number) jwt.getClaim(JwtService.CLAIM_USER_ID)).longValue();
        return decisionService.decidir(id, request, aprobadorId);
    }
}
