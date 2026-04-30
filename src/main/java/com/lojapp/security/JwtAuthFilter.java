package com.lojapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter implements Ordered {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 200;

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        header = header.trim();
        // OAuth permite "Bearer" case-insensitive; exige um único espaço após o esquema.
        if (header.length() < 7 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        jwtService
                .parseAccessToken(token)
                .ifPresentOrElse(
                        jwtUser -> {
                            var auth =
                                    new UsernamePasswordAuthenticationToken(
                                            jwtUser,
                                            null,
                                            jwtUser.authorities().stream()
                                                    .map(SimpleGrantedAuthority::new)
                                                    .collect(Collectors.toSet()));
                            auth.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        },
                        SecurityContextHolder::clearContext);
        chain.doFilter(request, response);
    }
}
