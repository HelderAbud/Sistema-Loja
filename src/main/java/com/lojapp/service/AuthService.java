package com.lojapp.service;

import com.lojapp.config.AuthRegistrationProperties;
import com.lojapp.config.JwtProperties;
import com.lojapp.entity.RefreshToken;
import com.lojapp.entity.User;
import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.dto.AuthDtos.UserMeResponse;
import com.lojapp.dto.user.AdminUserSummaryResponse;
import com.lojapp.repository.RefreshTokenRepository;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.observability.AuthSessionMetrics;
import com.lojapp.repository.UserRepository;
import com.lojapp.security.AppRole;
import com.lojapp.security.JwtService;
import com.lojapp.service.contract.AuthServiceContract;
import com.lojapp.util.TokenHashUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements AuthServiceContract {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokens;
    private final AuditService auditService;
    private final AuthRegistrationProperties registrationProperties;
    private final AuthSessionMetrics authSessionMetrics;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            RefreshTokenRepository refreshTokens,
            AuditService auditService,
            AuthRegistrationProperties registrationProperties,
            AuthSessionMetrics authSessionMetrics) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokens = refreshTokens;
        this.auditService = auditService;
        this.registrationProperties = registrationProperties;
        this.authSessionMetrics = authSessionMetrics;
    }

    @Transactional
    public IssuedAuthTokens register(RegisterRequest req) {
        assertRegistrationAllowed(req);
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new LojappDomainException(ApiErrorCode.CONFLICT, "Email já cadastrado");
        }
        User u = new User();
        u.setEmail(req.email().trim().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setAppRole(AppRole.USER.name());
        // Garantir que o id existe antes de audit/JWT (evita NPE ao desempacotar Long em createToken).
        users.saveAndFlush(u);
        auditService.log(u.getId(), "AUTH_REGISTER", u.getEmail());
        return issueTokens(u);
    }

    @Transactional
    public IssuedAuthTokens login(LoginRequest req) {
        User u =
                users.findByEmailIgnoreCase(req.email().trim().toLowerCase())
                        .orElseThrow(
                                () ->
                                        new LojappDomainException(
                                                ApiErrorCode.UNAUTHORIZED, "Credenciais inválidas"));
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new LojappDomainException(ApiErrorCode.UNAUTHORIZED, "Credenciais inválidas");
        }
        auditService.log(u.getId(), "AUTH_LOGIN", null);
        return issueTokens(u);
    }

    @Transactional
    public IssuedAuthTokens refresh(String rawRefresh) {
        String normalized = normalizeRefreshTokenOrEmpty(rawRefresh);
        if (normalized.isEmpty()) {
            authSessionMetrics.recordRefreshOutcome("invalid");
            throw new LojappDomainException(ApiErrorCode.UNAUTHORIZED, "Refresh token inválido");
        }
        Optional<RefreshToken> row =
                refreshTokens.findByTokenHash(TokenHashUtil.sha256Hex(normalized));
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
        IssuedAuthTokens issued = issueTokens(user);
        authSessionMetrics.recordRefreshOutcome("success");
        return issued;
    }

    @Transactional
    public void logout(String rawRefresh) {
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

    private void assertRegistrationAllowed(RegisterRequest req) {
        if (!registrationProperties.enabled()) {
            throw new LojappDomainException(
                    ApiErrorCode.FORBIDDEN, "O registo público está desativado.");
        }
        if (registrationProperties.inviteSecretConfigured()) {
            String expected = registrationProperties.inviteSecret().trim();
            String provided = req.inviteToken() == null ? "" : req.inviteToken().trim();
            if (!sha256HexEquals(provided, expected)) {
                throw new LojappDomainException(
                        ApiErrorCode.FORBIDDEN, "Convite de registo inválido ou ausente.");
            }
        }
        Set<String> allowed = registrationProperties.allowedDomainSet();
        if (allowed.isEmpty()) {
            return;
        }
        String email =
                req.email() == null ? "" : req.email().trim().toLowerCase(Locale.ROOT);
        int at = email.lastIndexOf('@');
        if (at < 1 || at == email.length() - 1) {
            throw new LojappDomainException(
                    ApiErrorCode.FORBIDDEN, "Email inválido para verificação de domínio.");
        }
        String domain = email.substring(at + 1);
        if (!allowed.contains(domain)) {
            throw new LojappDomainException(
                    ApiErrorCode.FORBIDDEN,
                    "Este domínio de email não está autorizado para registo.");
        }
    }

    private IssuedAuthTokens issueTokens(User u) {
        long userId = Objects.requireNonNull(u.getId(), "user id após persistência");
        String role = AppRole.fromStoredValue(u.getAppRole()).name();
        String access = jwtService.createToken(userId, u.getEmail(), role);
        refreshTokens.deleteByUser_Id(userId);
        String rawRefresh =
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", "");
        RefreshToken row = new RefreshToken();
        row.setUser(users.getReferenceById(userId));
        row.setTokenHash(TokenHashUtil.sha256Hex(rawRefresh));
        row.setExpiresAt(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
        refreshTokens.save(row);
        return new IssuedAuthTokens(access, rawRefresh);
    }

    private static boolean sha256HexEquals(String a, String b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return MessageDigest.isEqual(
                    md.digest(a.getBytes(StandardCharsets.UTF_8)),
                    md.digest(b.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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

    public UserMeResponse me(long userId) {
        User u =
                users.findById(userId)
                        .orElseThrow(
                                () ->
                                        new LojappDomainException(
                                                ApiErrorCode.NOT_FOUND, "Utilizador não encontrado"));
        return new UserMeResponse(
                u.getId(), u.getEmail(), AppRole.fromStoredValue(u.getAppRole()).name());
    }

    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> listUsersForAdmin(Pageable pageable) {
        return users.findAll(pageable).map(this::toAdminSummary);
    }

    private AdminUserSummaryResponse toAdminSummary(User u) {
        return new AdminUserSummaryResponse(
                u.getId(),
                u.getEmail(),
                AppRole.fromStoredValue(u.getAppRole()).name());
    }
}
