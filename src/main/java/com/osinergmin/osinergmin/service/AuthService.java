package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.LoginRequest;
import com.osinergmin.osinergmin.dto.LoginResponse;
import com.osinergmin.osinergmin.dto.UsuarioResponse;
import com.osinergmin.osinergmin.model.Usuario;
import com.osinergmin.osinergmin.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager, UsuarioService usuarioService,
            JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /** Valida usuario y contraseña (BCrypt). Lanza BadCredentials/Disabled si falla. */
    public Sesion login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));

        Usuario usuario = usuarioService.buscarPorUsername(auth.getName());
        return new Sesion(accessToken(usuario), refreshTokenService.crear(usuario));
    }

    /** Entrega un access token nuevo y rota el refresh token. */
    public Sesion refrescar(String refreshToken) {
        RefreshTokenService.Rotacion rotacion = refreshTokenService.rotar(refreshToken);
        return new Sesion(accessToken(rotacion.usuario()), rotacion.nuevoValor());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revocar(refreshToken);
    }

    private LoginResponse accessToken(Usuario usuario) {
        JwtService.TokenGenerado token = jwtService.generar(usuario);
        return new LoginResponse(token.token(), "Bearer", token.expiraEn(), UsuarioResponse.from(usuario));
    }

    /** Respuesta para el cuerpo (access token) + refresh token que va solo en la cookie. */
    public record Sesion(LoginResponse respuesta, String refreshToken) {
    }
}
