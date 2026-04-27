package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.exception.domain.LojappDomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Convite de registo — corre no perfil {@code ci-unit-tests} (nome sem sufixo {@code IntegrationTest}). */
@SpringBootTest
@Transactional
@TestPropertySource(
        properties = {
            "lojapp.auth.registration.enabled=true",
            "lojapp.auth.registration.invite-secret=pilot-invite-secret-32chars-min!!",
            "lojapp.auth.registration.allowed-domains=",
        })
class AuthRegistrationInvitePolicyTest {

    private static final String INVITE = "pilot-invite-secret-32chars-min!!";

    @Autowired private AuthService authService;

    @Test
    void register_withoutInvite_throwsForbidden() {
        String email = "inv-miss-" + UUID.randomUUID() + "@lojapp.test";
        assertThatThrownBy(
                        () -> authService.register(new RegisterRequest(email, "senha1234", null)))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.FORBIDDEN));
    }

    @Test
    void register_wrongInvite_throwsForbidden() {
        String email = "inv-wrong-" + UUID.randomUUID() + "@lojapp.test";
        assertThatThrownBy(
                        () ->
                                authService.register(
                                        new RegisterRequest(email, "senha1234", "wrong-value")))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.FORBIDDEN));
    }

    @Test
    void register_correctInvite_succeeds() {
        String email = "inv-ok-" + UUID.randomUUID() + "@lojapp.test";
        var issued = authService.register(new RegisterRequest(email, "senha1234", INVITE));
        assertThat(issued.accessToken()).isNotBlank();
        assertThat(issued.rawRefreshToken()).isNotBlank();
    }
}
