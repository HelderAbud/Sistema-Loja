package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lojapp.observability.AuthSessionMetrics;
import com.lojapp.repository.RefreshTokenRepository;
import com.lojapp.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshMetricsTest {

    @Mock private UserRepository users;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private AuditService auditService;
    @Mock private AuthTokenIssuerService authTokenIssuerService;

    @Test
    void refresh_unexpectedFailure_doesNotRecordOutcomeMetric() {
        var registry = new SimpleMeterRegistry();
        var authMetrics = new AuthSessionMetrics(registry);
        var refreshUseCase =
                new AuthRefreshUseCase(
                        refreshTokens, auditService, authTokenIssuerService, authMetrics, users);

        double before = registry.counter("lojapp.auth.refresh", "outcome", "unexpected").count();
        when(refreshTokens.findByTokenHash(anyString())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> refreshUseCase.execute("refresh-opaco-valido"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        assertThat(registry.counter("lojapp.auth.refresh", "outcome", "unexpected").count())
                .isEqualTo(before);
        verifyNoInteractions(authTokenIssuerService);
    }
}
