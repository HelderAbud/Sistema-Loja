package com.lojapp.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.entity.User;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.UserRepository;
import com.lojapp.application.nfe.ImportNfeUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        properties = {
            "lojapp.nfe.import.max-items=2",
            "lojapp.nfe.import.max-xml-chars=200"
        })
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class NfeImportGuardrailsIntegrationTest {

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

    @Autowired private ImportNfeUseCase importNfeUseCase;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void importNfe_rejectsWhenParsedItemsExceedConfiguredLimit() {
        User user = new User();
        user.setEmail("nfe-limit-" + UUID.randomUUID() + "@test.com");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user = users.save(user);
        final long userId = user.getId();

        String xml =
                """
                <nfe><nNF>1</nNF><xNome>Fornecedor</xNome><chNFe>35200111111111111111550010000010011000000000</chNFe>
                <prod><xProd>Item 1</xProd><qCom>1</qCom><vUnCom>1.00</vUnCom></prod>
                <prod><xProd>Item 2</xProd><qCom>1</qCom><vUnCom>1.00</vUnCom></prod>
                <prod><xProd>Item 3</xProd><qCom>1</qCom><vUnCom>1.00</vUnCom></prod>
                </nfe>
                """;

        assertThatThrownBy(() -> importNfeUseCase.execute(userId, xml))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void importNfe_rejectsWhenRawXmlExceedsConfiguredCharLimit() {
        User user = new User();
        user.setEmail("nfe-xml-limit-" + UUID.randomUUID() + "@test.com");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user = users.save(user);
        final long userId = user.getId();

        StringBuilder hugeXml = new StringBuilder("<nfe>");
        while (hugeXml.length() <= 220) {
            hugeXml.append("0123456789");
        }
        hugeXml.append("</nfe>");

        assertThatThrownBy(() -> importNfeUseCase.execute(userId, hugeXml.toString()))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void importNfe_rejectsWhenRawXmlIsBlank() {
        User user = new User();
        user.setEmail("nfe-blank-" + UUID.randomUUID() + "@test.com");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user = users.save(user);
        final long userId = user.getId();

        assertThatThrownBy(() -> importNfeUseCase.execute(userId, "   "))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void importNfe_rejectsWhenXmlIsMalformed() {
        User user = new User();
        user.setEmail("nfe-malformed-" + UUID.randomUUID() + "@test.com");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user = users.save(user);
        final long userId = user.getId();

        String malformedXml =
                """
                <nfe>
                  <nNF>10</nNF>
                  <xNome>Fornecedor</xNome>
                  <prod><xProd>Item</xProd><qCom>1</qCom><vUnCom>1.00</vUnCom>
                </nfe>
                """;

        assertThatThrownBy(() -> importNfeUseCase.execute(userId, malformedXml))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }
}
