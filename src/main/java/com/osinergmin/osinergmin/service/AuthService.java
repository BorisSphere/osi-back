package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.dto.LoginRequest;
import com.osinergmin.osinergmin.dto.LoginResponse;
import com.osinergmin.osinergmin.dto.UsuarioResponse;
import com.osinergmin.osinergmin.exception.EntraAccesoDenegadoException;
import com.osinergmin.osinergmin.exception.EntraTokenInvalidoException;
import com.osinergmin.osinergmin.model.Rol;
import com.osinergmin.osinergmin.model.Usuario;
import com.osinergmin.osinergmin.security.EntraProperties;
import com.osinergmin.osinergmin.security.EntraTokenValidator;
import com.osinergmin.osinergmin.security.JwtService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EntraTokenValidator entraTokenValidator;
    private final EntraProperties entraProperties;

    public AuthService(AuthenticationManager authenticationManager, UsuarioService usuarioService,
            JwtService jwtService, RefreshTokenService refreshTokenService,
            EntraTokenValidator entraTokenValidator, EntraProperties entraProperties) {
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.entraTokenValidator = entraTokenValidator;
        this.entraProperties = entraProperties;
    }

    /** Valida el token de Microsoft Entra ID y abre sesión con el JWT propio del sistema. */
    public Sesion loginEntra(String entraAccessToken) {
        Jwt jwt = entraTokenValidator.validar(entraAccessToken);

        String oid = jwt.getClaimAsString("oid");
        String correo = correo(jwt);
        if (oid == null || correo == null) {
            throw new EntraTokenInvalidoException("El token no incluye el identificador o el correo del usuario");
        }
        Rol rol = rolDesdeEntra(jwt)
                .orElseThrow(() -> new EntraAccesoDenegadoException("No tiene un rol asignado en la aplicación"));

        Usuario usuario = usuarioService.sincronizarDesdeEntra(oid, correo, rol, entraProperties.autoCrear());
        return new Sesion(accessToken(usuario), refreshTokenService.crear(usuario));
    }

    /** Primer claim con forma de correo entre email, preferred_username y upn. */
    private static String correo(Jwt jwt) {
        return Stream.of("email", "preferred_username", "upn")
                .map(jwt::getClaimAsString)
                .filter(valor -> valor != null && valor.contains("@"))
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .findFirst()
                .orElse(null);
    }

    /** Si el usuario tiene varios roles en Entra se aplica el de mayor privilegio. */
    private static Optional<Rol> rolDesdeEntra(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return Optional.empty();
        }
        return Stream.of(Rol.COORDINADOR, Rol.APROBADOR, Rol.ANALISTA)
                .filter(rol -> roles.contains(rol.name()))
                .findFirst();
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
