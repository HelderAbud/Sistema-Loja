package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.supplier.SupplierResponse;
import com.lojapp.exception.GlobalExceptionHandler;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.support.TestJwtAuth;
import com.lojapp.service.LojappHierarchyService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = SupplierController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class SupplierControllerTest {

    private static final long USER_ID = 88L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private LojappHierarchyService hierarchy;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private static RequestPostProcessor lojappUser(long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(TestJwtAuth.userToken(userId));
            return request;
        };
    }

    @Test
    void listSuppliers_returnsJson() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(hierarchy.listSuppliers(USER_ID))
                .thenReturn(
                        List.of(
                                new SupplierResponse(1L, "Forn A", "12345678000199", t, t),
                                new SupplierResponse(2L, "Forn B", null, t, t)));

        mockMvc.perform(
                        get("/api/v1/lojapp/suppliers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].legalName").value("Forn A"))
                .andExpect(jsonPath("$[0].taxId").value("12345678000199"))
                .andExpect(jsonPath("$[1].legalName").value("Forn B"));
    }

    @Test
    void createSupplier_blankLegalName_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/suppliers")
                                .contentType(APPLICATION_JSON)
                                .content("{\"legalName\":\"\",\"taxId\":null}")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_ERROR.code()));
    }

    @Test
    void getSupplier_delegatesToService() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(hierarchy.getSupplier(eq(USER_ID), eq(5L)))
                .thenReturn(new SupplierResponse(5L, "X", null, t, t));

        mockMvc.perform(
                        get("/api/v1/lojapp/suppliers/5")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.legalName").value("X"));
    }
}
