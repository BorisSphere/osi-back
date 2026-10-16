package com.osinergmin.osinergmin.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Crea la cookie HttpOnly del refresh token. Solo se envía a /api/auth (refresh y logout). */
@Component
public class RefreshCookieService {

    private static final String PATH = "/api/auth";

    private final RefreshTokenProperties properties;

    public RefreshCookieService(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    public String cookieName() {
        return properties.cookieName();
    }

    public ResponseCookie crear(String refreshToken) {
        return base(refreshToken).maxAge(properties.expiration()).build();
    }

    public ResponseCookie limpiar() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }
}
