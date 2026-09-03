package com.lojapp.service;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLoginUseCase {

    static final String TIMING_PAD_PASSWORD = "lojapp-timing-pad";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AuthTokenIssuerService authTokenIssuerService;
    private final String dummyPasswordHash;

    public AuthLoginUseCase(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            AuthTokenIssuerService authTokenIssuerService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.authTokenIssuerService = authTokenIssuerService;
        this.dummyPasswordHash = passwordEncoder.encode(TIMING_PAD_PASSWORD);
    }

    @Transactional
    public IssuedAuthTokens execute(LoginRequest req) {
        Optional<User> found =
                users.findByEmailIgnoreCase(req.email().trim().toLowerCase());
        boolean passwordOk =
                passwordEncoder.matches(
                        req.password(), found.map(User::getPasswordHash).orElse(dummyPasswordHash));
        if (found.isEmpty() || !passwordOk) {
            throw new LojappDomainException(ApiErrorCode.UNAUTHORIZED, "Credenciais inválidas");
        }
        User user = found.get();
        auditService.log(user.getId(), "AUTH_LOGIN", null);
        return authTokenIssuerService.issueTokens(user);
    }
}
