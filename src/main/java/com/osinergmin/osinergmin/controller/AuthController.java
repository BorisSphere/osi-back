package com.osinergmin.osinergmin.controller;

import com.osinergmin.osinergmin.dto.LoginRequest;
import com.osinergmin.osinergmin.dto.LoginResponse;
import com.osinergmin.osinergmin.dto.UsuarioResponse;
import com.osinergmin.osinergmin.security.RefreshCookieService;
import com.osinergmin.osinergmin.service.AuthService;
import com.osinergmin.osinergmin.service.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación:
 * - login/refresh devuelven el access token (JWT, vida corta) en el cuerpo y el
 *   refresh token en una cookie HttpOnly limitada a /api/auth.
 * - El access token se envía en cada petición como "Authorization: Bearer <token>".
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final RefreshCookieService cookieService;

    public AuthController(AuthService authService, UsuarioService usuarioService,
            RefreshCookieService cookieService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.cookieService = cookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return conCookie(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        return conCookie(authService.refrescar(leerCookie(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(leerCookie(request));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.limpiar().toString())
                .build();
    }

    /** Cualquier usuario autenticado: devuelve sus propios datos a partir del token. */
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.from(usuarioService.buscarPorUsername(jwt.getSubject()));
    }

    private ResponseEntity<LoginResponse> conCookie(AuthService.Sesion sesion) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.crear(sesion.refreshToken()).toString())
                .body(sesion.respuesta());
    }

    // Lectura manual: @CookieValue no admite un nombre configurable por propiedad record.
    private String leerCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieService.cookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
