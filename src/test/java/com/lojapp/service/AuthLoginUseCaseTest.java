package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthLoginUseCaseTest {

    private static final String TIMING_PAD = AuthLoginUseCase.TIMING_PAD_PASSWORD;
    private static final String DUMMY_HASH = "$2a$12$dummyTimingHashForTests.........";

    @Mock private UserRepository users;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private AuthTokenIssuerService authTokenIssuerService;

    private AuthLoginUseCase useCase;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(TIMING_PAD)).thenReturn(DUMMY_HASH);
        useCase = new AuthLoginUseCase(users, passwordEncoder, auditService, authTokenIssuerService);
    }

    @Test
    void unknownEmail_stillRunsPasswordMatchesAgainstDummyHash() {
        when(users.findByEmailIgnoreCase("ghost@lojapp.test")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("qualquer", DUMMY_HASH)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new LoginRequest("ghost@lojapp.test", "qualquer")))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.UNAUTHORIZED));

        verify(passwordEncoder).matches("qualquer", DUMMY_HASH);
        verifyNoInteractions(authTokenIssuerService);
        verifyNoInteractions(auditService);
    }

    @Test
    void knownEmailWrongPassword_doesNotIssueTokens() {
        User user = new User();
        user.setId(9L);
        user.setPasswordHash("$2a$12$realhash");
        when(users.findByEmailIgnoreCase("ana@lojapp.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new LoginRequest("ana@lojapp.test", "errada")))
                .isInstanceOf(LojappDomainException.class);

        verify(passwordEncoder).matches("errada", "$2a$12$realhash");
        verifyNoInteractions(authTokenIssuerService);
    }

    @Test
    void validCredentials_issuesTokens() {
        User user = new User();
        user.setId(3L);
        user.setPasswordHash("$2a$12$realhash");
        when(users.findByEmailIgnoreCase("ok@lojapp.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha1234", user.getPasswordHash())).thenReturn(true);
        when(authTokenIssuerService.issueTokens(user))
                .thenReturn(new IssuedAuthTokens("at", "rt"));

        IssuedAuthTokens tokens = useCase.execute(new LoginRequest("ok@lojapp.test", "senha1234"));

        assertThat(tokens.accessToken()).isEqualTo("at");
        verify(auditService).log(3L, "AUTH_LOGIN", null);
    }
}
