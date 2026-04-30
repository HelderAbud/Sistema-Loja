package com.lojapp.service;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLoginUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AuthTokenIssuerService authTokenIssuerService;

    public AuthLoginUseCase(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            AuthTokenIssuerService authTokenIssuerService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.authTokenIssuerService = authTokenIssuerService;
    }

    @Transactional
    public IssuedAuthTokens execute(LoginRequest req) {
        User user =
                users.findByEmailIgnoreCase(req.email().trim().toLowerCase())
                        .orElseThrow(
                                () ->
                                        new LojappDomainException(
                                                ApiErrorCode.UNAUTHORIZED, "Credenciais inválidas"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new LojappDomainException(ApiErrorCode.UNAUTHORIZED, "Credenciais inválidas");
        }
        auditService.log(user.getId(), "AUTH_LOGIN", null);
        return authTokenIssuerService.issueTokens(user);
    }
}
