package com.lojapp.service;

import com.lojapp.config.AuthRegistrationProperties;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.UserRepository;
import com.lojapp.security.AppRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthRegisterUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AuthTokenIssuerService authTokenIssuerService;
    private final AuthRegistrationProperties registrationProperties;

    public AuthRegisterUseCase(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            AuthTokenIssuerService authTokenIssuerService,
            AuthRegistrationProperties registrationProperties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.authTokenIssuerService = authTokenIssuerService;
        this.registrationProperties = registrationProperties;
    }

    @Transactional
    public IssuedAuthTokens execute(RegisterRequest req) {
        assertRegistrationAllowed(req);
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new LojappDomainException(ApiErrorCode.CONFLICT, "Email já cadastrado");
        }
        User u = new User();
        u.setEmail(req.email().trim().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setAppRole(AppRole.USER.name());
        users.saveAndFlush(u);
        auditService.log(u.getId(), "AUTH_REGISTER", u.getEmail());
        return authTokenIssuerService.issueTokens(u);
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
        String email = req.email() == null ? "" : req.email().trim().toLowerCase(Locale.ROOT);
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
}
