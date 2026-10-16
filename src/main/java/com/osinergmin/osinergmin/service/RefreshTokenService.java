package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.exception.RefreshTokenInvalidoException;
import com.osinergmin.osinergmin.model.RefreshToken;
import com.osinergmin.osinergmin.model.Usuario;
import com.osinergmin.osinergmin.repository.RefreshTokenRepository;
import com.osinergmin.osinergmin.security.RefreshTokenProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Refresh tokens opacos con rotación: cada uso entrega uno nuevo e invalida el anterior.
 * Si un token ya rotado se vuelve a usar fuera del margen de gracia, se asume robo y se
 * revocan todas las sesiones del usuario.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final RefreshTokenProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, RefreshTokenProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Crea un refresh token para el usuario y devuelve el valor en claro (solo viaja en la cookie). */
    @Transactional
    public String crear(Usuario usuario) {
        String valor = generarValor();
        Instant ahora = Instant.now();

        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(hash(valor));
        token.setFechaCreacion(ahora);
        token.setFechaExpiracion(ahora.plus(properties.expiration()));
        token.setRevocado(false);
        repository.save(token);
        return valor;
    }

    /** Valida el token recibido, lo marca como usado y entrega uno nuevo para el mismo usuario. */
    @Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
    public Rotacion rotar(String valor) {
        RefreshToken actual = buscar(valor);
        Instant ahora = Instant.now();
        Usuario usuario = actual.getUsuario();

        if (actual.getRevocado()) {
            throw new RefreshTokenInvalidoException("Sesión cerrada");
        }
        if (actual.getFechaExpiracion().isBefore(ahora)) {
            throw new RefreshTokenInvalidoException("Sesión expirada");
        }
        if (actual.getFechaUso() != null
                && actual.getFechaUso().plus(properties.reuseGrace()).isBefore(ahora)) {
            repository.revocarTodosDelUsuario(usuario.getId());
            log.warn("Reutilización de refresh token del usuario '{}': se revocaron todas sus sesiones",
                    usuario.getUsername());
            throw new RefreshTokenInvalidoException("Sesión inválida");
        }
        if (!usuario.getActivo()) {
            repository.revocarTodosDelUsuario(usuario.getId());
            throw new RefreshTokenInvalidoException("El usuario está inactivo");
        }

        if (actual.getFechaUso() == null) {
            actual.setFechaUso(ahora);
        }
        return new Rotacion(usuario, crear(usuario));
    }

    /** Logout: revoca el token si existe. No falla si ya no es válido. */
    @Transactional
    public void revocar(String valor) {
        if (!StringUtils.hasText(valor)) {
            return;
        }
        repository.findByTokenHash(hash(valor)).ifPresent(token -> token.setRevocado(true));
    }

    /** Borra cada madrugada los tokens vencidos hace más de un día. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limpiarExpirados() {
        int eliminados = repository.eliminarExpiradosAntesDe(Instant.now().minusSeconds(86_400));
        if (eliminados > 0) {
            log.info("Se eliminaron {} refresh tokens expirados", eliminados);
        }
    }

    private RefreshToken buscar(String valor) {
        if (!StringUtils.hasText(valor)) {
            throw new RefreshTokenInvalidoException("No hay sesión activa");
        }
        return repository.findByTokenHash(hash(valor))
                .orElseThrow(() -> new RefreshTokenInvalidoException("Sesión inválida"));
    }

    private static String generarValor() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Rotacion(Usuario usuario, String nuevoValor) {
    }
}
