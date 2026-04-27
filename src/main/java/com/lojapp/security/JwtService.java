package com.lojapp.security;

import com.lojapp.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties props;
    private final SecretKey key;
    private final MeterRegistry meterRegistry;

    public JwtService(JwtProperties props, MeterRegistry meterRegistry) {
        this.props = props;
        this.meterRegistry = meterRegistry;
        byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("LOJAPP_JWT_SECRET must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(long userId, String email, String appRole) {
        long now = System.currentTimeMillis();
        String normalizedRole = AppRole.fromStoredValue(appRole).name();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", normalizedRole)
                .issuedAt(new Date(now))
                .expiration(new Date(now + props.expirationMs()))
                .signWith(key)
                .compact();
    }

    public Optional<JwtUser> parseAccessToken(String token) {
        try {
            Claims claims =
                    Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            long userId = Long.parseLong(claims.getSubject());
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);
            String normalizedRole = AppRole.fromStoredValue(role).name();
            return Optional.of(new JwtUser(userId, email != null ? email : "", normalizedRole));
        } catch (ExpiredJwtException ex) {
            incrementJwtParseFailure("expired");
            log.debug("JWT access expirado");
            return Optional.empty();
        } catch (MalformedJwtException ex) {
            incrementJwtParseFailure("malformed");
            log.warn("JWT malformado: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        } catch (JwtException ex) {
            incrementJwtParseFailure("invalid");
            log.warn("JWT inválido: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        } catch (IllegalArgumentException ex) {
            incrementJwtParseFailure("malformed");
            log.warn("JWT rejeitado (claims): {}", ex.getClass().getSimpleName());
            return Optional.empty();
        } catch (Exception ex) {
            incrementJwtParseFailure("unexpected");
            log.warn(
                    "Falha inesperada ao interpretar JWT (tipo={}): {}",
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            return Optional.empty();
        }
    }

    private void incrementJwtParseFailure(String reason) {
        meterRegistry.counter("lojapp.jwt.access.parse.failed", "reason", reason).increment();
    }
}
