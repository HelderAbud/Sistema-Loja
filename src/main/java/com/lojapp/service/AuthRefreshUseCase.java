package com.lojapp.service;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.entity.RefreshToken;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.observability.AuthSessionMetrics;
import com.lojapp.repository.RefreshTokenRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.util.TokenHashUtil;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthRefreshUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final AuditService auditService;
    private final AuthTokenIssuerService authTokenIssuerService;
    private final AuthSessionMetrics authSessionMetrics;
    private final UserRepository users;

    public AuthRefreshUseCase(
            RefreshTokenRepository refreshTokens,
            AuditService auditService,
            AuthTokenIssuerService authTokenIssuerService,
            AuthSessionMetrics authSessionMetrics,
            UserRepository users) {
        this.refreshTokens = refreshTokens;
        this.auditService = auditService;
        this.authTokenIssuerService = authTokenIssuerService;
        this.authSessionMetrics = authSessionMetrics;
        this.users = users;
    }

    @Transactional
    public IssuedAuthTokens execute(String rawRefresh) {
        String normalized = normalizeRefreshTokenOrEmpty(rawRefresh);
        if (normalized.isEmpty()) {
            authSessionMetrics.recordRefreshOutcome("invalid");
            throw new LojappDomainException(ApiErrorCode.UNAUTHORIZED, "Refresh token inválido");
        }
        Optional<RefreshToken> row = refreshTokens.findByTokenHash(TokenHashUtil.sha256Hex(normalized));
        if (row.isEmpty()) {
            authSessionMetrics.recordRefreshOutcome("invalid");
            throw new LojappDomainException(ApiErrorCode.UNAUTHORIZED, "Refresh token inválido");
        }
        RefreshToken stored = row.get();
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokens.delete(stored);
            authSessionMetrics.recordRefreshOutcome("expired");
            throw new LojappDomainException(ApiErrorCode.UNAUTHORIZED, "Refresh token expirado");
        }
        User user = loadRefreshTokenOwner(stored);
        refreshTokens.delete(stored);
        auditService.log(user.getId(), "AUTH_REFRESH", null);
        IssuedAuthTokens issued = authTokenIssuerService.issueTokens(user);
        authSessionMetrics.recordRefreshOutcome("success");
        return issued;
    }

    private User loadRefreshTokenOwner(RefreshToken stored) {
        long userId = stored.getUser().getId();
        return users.findById(userId)
                .orElseThrow(
                        () ->
                                new LojappDomainException(
                                        ApiErrorCode.UNAUTHORIZED, "Utilizador inválido"));
    }

    private String normalizeRefreshTokenOrEmpty(String rawRefresh) {
        return rawRefresh == null ? "" : rawRefresh.trim();
    }
}
