package com.lojapp.config;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Define o refresh token opaco como cookie HttpOnly (não legível por JavaScript), com path restrito
 * a {@code /api/v1/auth} para não ir em cada pedido à API de negócio.
 */
@Component
public class AuthRefreshCookieSupport {

    private final String cookieName;
    private final String cookiePath;
    private final boolean secure;
    private final long refreshTtlMs;

    public AuthRefreshCookieSupport(
            JwtProperties jwtProperties,
            @Value("${lojapp.auth.refresh-cookie-name:lojapp_rt}") String cookieName,
            @Value("${lojapp.auth.refresh-cookie-path:/api/v1/auth}") String cookiePath,
            @Value("${lojapp.auth.refresh-cookie-secure:false}") boolean secure) {
        this.refreshTtlMs = jwtProperties.refreshExpirationMs();
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.secure = secure;
    }

    public String cookieName() {
        return cookieName;
    }

    public void writeRefreshCookie(HttpServletResponse response, String rawRefreshToken) {
        long maxAgeSec = Math.max(1L, Duration.ofMillis(refreshTtlMs).getSeconds());
        ResponseCookie cookie =
                ResponseCookie.from(cookieName, rawRefreshToken)
                        .httpOnly(true)
                        .secure(secure)
                        .path(cookiePath)
                        .maxAge(maxAgeSec)
                        .sameSite("Lax")
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie =
                ResponseCookie.from(cookieName, "")
                        .httpOnly(true)
                        .secure(secure)
                        .path(cookiePath)
                        .maxAge(0)
                        .sameSite("Lax")
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
