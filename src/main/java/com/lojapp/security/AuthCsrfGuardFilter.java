package com.lojapp.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guarda simples anti-CSRF para operações sensíveis de autenticação via cookie.
 * Rejeita refresh/logout quando a origem não é permitida.
 */
@Component
public class AuthCsrfGuardFilter extends OncePerRequestFilter implements Ordered {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 90;

    private final Set<String> allowedOrigins;
    private final String refreshCookieName;

    public AuthCsrfGuardFilter(
            @Value("${lojapp.cors.allowed-origins}") String allowedOriginsCsv,
            @Value("${lojapp.auth.refresh-cookie-name:lojapp_rt}") String refreshCookieName) {
        this.allowedOrigins =
                Arrays.stream(allowedOriginsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        this.refreshCookieName = refreshCookieName;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isProtectedAuthWrite(request) || !hasRefreshCookie(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = trimToNull(request.getHeader("Origin"));
        String referer = trimToNull(request.getHeader("Referer"));
        if (origin != null && isAllowedOrigin(origin)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (origin == null && referer != null && isAllowedByReferer(referer)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Origem não autorizada para operação sensível de autenticação.");
    }

    private static boolean isProtectedAuthWrite(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri != null
                && (uri.endsWith("/api/v1/auth/refresh") || uri.endsWith("/api/v1/auth/logout"));
    }

    private boolean hasRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (refreshCookieName.equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedByReferer(String referer) {
        try {
            URI uri = URI.create(referer);
            String origin = uri.getScheme() + "://" + uri.getAuthority();
            return isAllowedOrigin(origin);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isAllowedOrigin(String origin) {
        return allowedOrigins.contains(origin);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
