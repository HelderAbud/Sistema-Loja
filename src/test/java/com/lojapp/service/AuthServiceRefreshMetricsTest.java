package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.lojapp.config.AuthRegistrationProperties;
import com.lojapp.config.JwtProperties;
import com.lojapp.observability.AuthSessionMetrics;
import com.lojapp.repository.RefreshTokenRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.security.JwtService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshMetricsTest {

    @Mock private UserRepository users;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private AuditService auditService;

    @Test
    void refresh_unexpectedFailure_doesNotRecordOutcomeMetric() {
        var registry = new SimpleMeterRegistry();
        var authMetrics = new AuthSessionMetrics(registry);
        var jwtProperties = new JwtProperties("01234567890123456789012345678901", 900_000L, 1_209_600_000L);
        var registrationProperties = new AuthRegistrationProperties(true, "", 10, "");
        var authService =
                new AuthService(
                        users,
                        passwordEncoder,
                        jwtService,
                        jwtProperties,
                        refreshTokens,
                        auditService,
                        registrationProperties,
                        authMetrics);

        double before = registry.counter("lojapp.auth.refresh", "outcome", "unexpected").count();
        when(refreshTokens.findByTokenHash(anyString())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> authService.refresh("refresh-opaco-valido"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        assertThat(registry.counter("lojapp.auth.refresh", "outcome", "unexpected").count())
                .isEqualTo(before);
    }
}
