package com.osinergmin.osinergmin.security;

import com.osinergmin.osinergmin.model.Usuario;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Genera los JWT. La validación la hace Spring Security con el JwtDecoder de SecurityConfig. */
@Service
public class JwtService {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_USER_ID = "uid";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public TokenGenerado generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(properties.expiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(usuario.getUsername())
                .claim(CLAIM_USER_ID, usuario.getId())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenGenerado(token, expira);
    }

    public record TokenGenerado(String token, Instant expiraEn) {
    }
}
