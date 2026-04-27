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
@TestPropertySource(properties = "lojapp.auth.registration.enabled=false")
class AuthRegistrationDisabledIntegrationTest {

    @Autowired private AuthService authService;

    @Test
    void register_throwsForbidden() {
        assertThatThrownBy(
                        () ->
                                authService.register(
                                        new RegisterRequest("novo@lojapp.test", "senha1234", null)))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.FORBIDDEN));
    }
}
