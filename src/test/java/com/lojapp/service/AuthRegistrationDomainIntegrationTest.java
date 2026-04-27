package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.exception.domain.LojappDomainException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(
        properties = {
            "lojapp.auth.registration.enabled=true",
            "lojapp.auth.registration.allowed-domains=lojapp.test, acme.org",
        })
class AuthRegistrationDomainIntegrationTest {

    @Autowired private AuthService authService;

    @Test
    void register_firstListedDomain_succeeds() {
        authService.register(new RegisterRequest("pilot@LOJAPP.TEST", "senha1234", null));
    }

    @Test
    void register_secondListedDomain_succeeds() {
        authService.register(new RegisterRequest("u@acme.org", "senha1234", null));
    }

    @Test
    void register_disallowedDomain_throwsForbidden() {
        assertThatThrownBy(
                        () ->
                                authService.register(
                                        new RegisterRequest("x@gmail.com", "senha1234", null)))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.FORBIDDEN));
    }
}
