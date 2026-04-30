package com.lojapp.service;

import com.lojapp.repository.RefreshTokenRepository;
import com.lojapp.util.TokenHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLogoutUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final AuditService auditService;

    public AuthLogoutUseCase(RefreshTokenRepository refreshTokens, AuditService auditService) {
        this.refreshTokens = refreshTokens;
        this.auditService = auditService;
    }

    @Transactional
    public void execute(String rawRefresh) {
        String normalized = normalizeRefreshTokenOrEmpty(rawRefresh);
        if (normalized.isEmpty()) {
            return;
        }

        refreshTokens
                .findByTokenHash(TokenHashUtil.sha256Hex(normalized))
                .ifPresent(
                        stored -> {
                            Long userId = stored.getUser().getId();
                            refreshTokens.delete(stored);
                            if (userId != null) {
                                auditService.log(userId, "AUTH_LOGOUT", "Refresh token revogado");
                            }
                        });
    }

    private String normalizeRefreshTokenOrEmpty(String rawRefresh) {
        return rawRefresh == null ? "" : rawRefresh.trim();
    }
}
