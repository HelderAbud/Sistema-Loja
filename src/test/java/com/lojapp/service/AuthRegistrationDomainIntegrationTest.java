package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.exception.domain.LojappDomainException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(
        properties = {
            "lojapp.auth.registration.enabled=true",
            "lojapp.auth.registration.allowed-domains=lojapp.test, acme.org",
        })
class AuthRegistrationDomainIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("lojapp")
                    .withUsername("lojapp")
                    .withPassword("lojapp_test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("lojapp.jwt.secret", () -> "integration-test-secret-32-chars-min!!");
    }

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
