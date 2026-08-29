package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.commission.CommissionAccrualResponse;
import com.lojapp.exception.GlobalExceptionHandler;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.service.contract.CommissionReportServiceContract;
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

@WebMvcTest(controllers = CommissionAccrualController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class CommissionAccrualControllerTest {

    private static final long USER_ID = 88L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private CommissionReportServiceContract report;

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
    void list_returnsJson() throws Exception {
        Instant created = Instant.parse("2026-08-15T12:00:00Z");
        when(report.list(eq(USER_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(
                        List.of(
                                new CommissionAccrualResponse(
                                        5L,
                                        88L,
                                        11L,
                                        "Ana",
                                        8L,
                                        "MarcaX",
                                        new BigDecimal("20.00"),
                                        new BigDecimal("12.0000"),
                                        new BigDecimal("2.40"),
                                        created)));

        mockMvc.perform(
                        get("/api/v1/lojapp/commission-accruals")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sellerName").value("Ana"))
                .andExpect(jsonPath("$[0].amount").value(2.40));
    }

    @Test
    void list_withCashierRole_returnsForbidden() throws Exception {
        mockMvc.perform(
                        get("/api/v1/lojapp/commission-accruals")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappCashier(USER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void csv_returnsTextCsv() throws Exception {
        when(report.toCsv(eq(USER_ID), any(Instant.class), any(Instant.class)))
                .thenReturn("id,saleId\n5,88\n");

        mockMvc.perform(
                        get("/api/v1/lojapp/commission-accruals.csv")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string("id,saleId\n5,88\n"));
    }
}
