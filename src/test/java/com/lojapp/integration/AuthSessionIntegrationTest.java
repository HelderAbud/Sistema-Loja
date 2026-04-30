package com.lojapp.integration;

import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthSessionIntegrationTest {

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

    @Autowired private MockMvc mockMvc;

    @Test
    void refresh_rotatesCookieAndInvalidatesPreviousRefreshToken() throws Exception {
        String email = "auth-session-" + UUID.randomUUID() + "@lojapp.test";

        var registerResult =
                mockMvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"%s","password":"senha1234"}
                                                """
                                                        .formatted(email)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                        .andReturn();

        String firstRefreshCookie = registerResult.getResponse().getHeader(SET_COOKIE);
        Cookie firstCookie = toCookie(firstRefreshCookie);

        var refreshResult =
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(APPLICATION_JSON)
                                        .content("{}")
                                        .header("Origin", "http://localhost:3000")
                                        .cookie(firstCookie))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                        .andReturn();

        String rotatedRefreshCookie = refreshResult.getResponse().getHeader(SET_COOKIE);
        Cookie rotatedCookie = toCookie(rotatedRefreshCookie);

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content("{}")
                                .header("Origin", "http://localhost:3000")
                                .cookie(firstCookie))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content("{}")
                                .header("Origin", "http://localhost:3000")
                                .cookie(rotatedCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    private static Cookie toCookie(String setCookieHeader) {
        String nameValue = setCookieHeader.split(";", 2)[0];
        String[] pair = nameValue.split("=", 2);
        return new Cookie(pair[0], pair.length > 1 ? pair[1] : "");
    }
}
