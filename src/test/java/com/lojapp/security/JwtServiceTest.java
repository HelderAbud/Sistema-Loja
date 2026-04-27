package com.lojapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "integration-test-secret-32-chars-min!!";

    @Test
    void parseValidToken_recordsNoFailure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JwtProperties props = new JwtProperties(SECRET, 3600_000L, 86_400_000L);
        JwtService svc = new JwtService(props, registry);

        String token = svc.createToken(42L, "user@example.com", "LOJA_USER");
        Optional<JwtUser> parsed = svc.parseAccessToken(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(42L);
        assertThat(parsed.get().email()).isEqualTo("user@example.com");
        assertThat(registry.find("lojapp.jwt.access.parse.failed").counters()).isEmpty();
    }

    @Test
    void parseExpiredToken_incrementsExpiredCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JwtProperties props = new JwtProperties(SECRET, -1L, 86_400_000L);
        JwtService svc = new JwtService(props, registry);

        String token = svc.createToken(1L, "a@b.co", "LOJA_USER");
        assertThat(svc.parseAccessToken(token)).isEmpty();

        assertThat(
                        registry.counter("lojapp.jwt.access.parse.failed", "reason", "expired")
                                .count())
                .isEqualTo(1.0);
    }

    @Test
    void parseWrongSignature_incrementsInvalidCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JwtService signer =
                new JwtService(
                        new JwtProperties(SECRET, 3600_000L, 86_400_000L), registry);
        JwtService otherParser =
                new JwtService(
                        new JwtProperties(SECRET.replace('!', '?'), 3600_000L, 86_400_000L),
                        registry);

        String token = signer.createToken(9L, "x@y.z", "LOJA_USER");
        assertThat(otherParser.parseAccessToken(token)).isEmpty();

        assertThat(
                        registry.counter("lojapp.jwt.access.parse.failed", "reason", "invalid")
                                .count())
                .isEqualTo(1.0);
    }

    @Test
    void parseMalformedToken_incrementsMalformedCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JwtService svc =
                new JwtService(new JwtProperties(SECRET, 3600_000L, 86_400_000L), registry);

        assertThat(svc.parseAccessToken("nÃ£o-Ã©-um-jwt")).isEmpty();

        assertThat(
                        registry.counter("lojapp.jwt.access.parse.failed", "reason", "malformed")
                                .count())
                .isEqualTo(1.0);
    }

    @Test
    void parseJwtWithNonNumericSubject_incrementsMalformedCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JwtProperties props = new JwtProperties(SECRET, 3600_000L, 86_400_000L);
        JwtService svc = new JwtService(props, registry);

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        String token =
                Jwts.builder()
                        .subject("not-a-number")
                        .claim("email", "a@b.co")
                        .claim("role", "LOJA_USER")
                        .issuedAt(new Date(now))
                        .expiration(new Date(now + 3600_000L))
                        .signWith(key)
                        .compact();

        assertThat(svc.parseAccessToken(token)).isEmpty();
        assertThat(
                        registry.counter("lojapp.jwt.access.parse.failed", "reason", "malformed")
                                .count())
                .isEqualTo(1.0);
    }
}
