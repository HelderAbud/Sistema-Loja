package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.commission.CommissionRuleRequest;
import com.lojapp.dto.commission.CommissionRuleResponse;
import com.lojapp.exception.GlobalExceptionHandler;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.service.contract.CommissionCatalogServiceContract;
import com.lojapp.support.TestJwtAuth;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = CommissionRuleController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class CommissionRuleControllerTest {

    private static final long USER_ID = 88L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private CommissionCatalogServiceContract catalog;

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

    private static RequestPostProcessor lojappCashier(long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(TestJwtAuth.cashierToken(userId));
            return request;
        };
    }

    @Test
    void listRules_returnsJson() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(catalog.listRules(USER_ID))
                .thenReturn(
                        List.of(
                                new CommissionRuleResponse(
                                        1L, null, new BigDecimal("5.0000"), t, t)));

        mockMvc.perform(
                        get("/api/v1/lojapp/commission-rules")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].percent").value(5.0000));
    }

    @Test
    void listRules_withCashierRole_returnsForbidden() throws Exception {
        mockMvc.perform(
                        get("/api/v1/lojapp/commission-rules")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappCashier(USER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRule_withCashierRole_returnsForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/commission-rules")
                                .contentType(APPLICATION_JSON)
                                .content("{\"percent\":5.0}")
                                .with(lojappCashier(USER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRule_delegatesToService() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(catalog.createRule(eq(USER_ID), org.mockito.ArgumentMatchers.any(CommissionRuleRequest.class)))
                .thenReturn(
                        new CommissionRuleResponse(2L, 8L, new BigDecimal("12.0000"), t, t));

        mockMvc.perform(
                        post("/api/v1/lojapp/commission-rules")
                                .contentType(APPLICATION_JSON)
                                .content("{\"brandId\":8,\"percent\":12.0}")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandId").value(8));
    }
}
