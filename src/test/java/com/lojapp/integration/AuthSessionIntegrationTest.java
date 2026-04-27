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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSessionIntegrationTest {

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
