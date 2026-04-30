package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.application.cash.CloseCashSessionUseCase;
import com.lojapp.application.cash.GetCashSessionClosePreviewUseCase;
import com.lojapp.application.cash.GetCurrentCashSessionUseCase;
import com.lojapp.application.cash.OpenCashSessionUseCase;
import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.cash.CloseCashSessionPreviewResponse;
import com.lojapp.dto.cash.CloseCashSessionResponse;
import com.lojapp.dto.cash.CurrentCashSessionResponse;
import com.lojapp.dto.cash.OpenCashSessionResponse;
import com.lojapp.exception.domain.CashSessionAlreadyOpenException;
import com.lojapp.exception.domain.CashSessionDifferenceReasonRequiredException;
import com.lojapp.exception.domain.CashSessionManagerApprovalRequiredException;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.support.TestJwtAuth;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = CashSessionController.class)
@Import(MethodSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class CashSessionControllerTest {

    private static final long USER_ID = 42L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private OpenCashSessionUseCase openCashSessionUseCase;

    @MockBean private CloseCashSessionUseCase closeCashSessionUseCase;

    @MockBean private GetCurrentCashSessionUseCase getCurrentCashSessionUseCase;

    @MockBean private GetCashSessionClosePreviewUseCase getCashSessionClosePreviewUseCase;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private static RequestPostProcessor lojappUser(long userId) {
        return new RequestPostProcessor() {
            @Override
            public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                SecurityContextHolder.getContext().setAuthentication(TestJwtAuth.userToken(userId));
                return request;
            }
        };
    }

    private static RequestPostProcessor lojappSeller(long userId) {
        return new RequestPostProcessor() {
            @Override
            public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                SecurityContextHolder.getContext().setAuthentication(TestJwtAuth.sellerToken(userId));
                return request;
            }
        };
    }

    @Test
    void openCashSession_whenValid_returnsOk() throws Exception {
        Instant openedAt = Instant.parse("2026-04-28T14:00:00Z");
        when(openCashSessionUseCase.execute(eq(USER_ID), eq(USER_ID), any()))
                .thenReturn(new OpenCashSessionResponse(7L, new BigDecimal("100.00"), openedAt, "OPEN"));

        mockMvc.perform(
                        post("/api/v1/lojapp/pos/cash-sessions/open")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"openingAmount\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashSessionId").value(7))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void openCashSession_whenAlreadyOpen_returnsConflict() throws Exception {
        when(openCashSessionUseCase.execute(eq(USER_ID), eq(USER_ID), any()))
                .thenThrow(new CashSessionAlreadyOpenException());

        mockMvc.perform(
                        post("/api/v1/lojapp/pos/cash-sessions/open")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"openingAmount\":50.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void closeCashSession_whenDifferenceWithoutReason_returnsBadRequest() throws Exception {
        when(closeCashSessionUseCase.execute(eq(USER_ID), eq(USER_ID), any()))
                .thenThrow(new CashSessionDifferenceReasonRequiredException());

        mockMvc.perform(
                        post("/api/v1/lojapp/pos/cash-sessions/close")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "cashSessionId": 7,
                                          "countedAmount": 90.00,
                                          "differenceReason": "",
                                          "managerApproval": false
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void closeCashSession_whenAboveToleranceWithoutApproval_returnsForbidden() throws Exception {
        when(closeCashSessionUseCase.execute(eq(USER_ID), eq(USER_ID), any()))
                .thenThrow(new CashSessionManagerApprovalRequiredException());

        mockMvc.perform(
                        post("/api/v1/lojapp/pos/cash-sessions/close")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "cashSessionId": 7,
                                          "countedAmount": 80.00,
                                          "differenceReason": "Diferença relevante",
                                          "managerApproval": false
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void closeCashSession_whenValid_returnsOk() throws Exception {
        Instant closedAt = Instant.parse("2026-04-28T16:00:00Z");
        when(closeCashSessionUseCase.execute(eq(USER_ID), eq(USER_ID), any()))
                .thenReturn(
                        new CloseCashSessionResponse(
                                7L,
                                new BigDecimal("100.00"),
                                new BigDecimal("100.00"),
                                BigDecimal.ZERO,
                                closedAt,
                                "CLOSED"));

        mockMvc.perform(
                        post("/api/v1/lojapp/pos/cash-sessions/close")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "cashSessionId": 7,
                                          "countedAmount": 100.00,
                                          "differenceReason": null,
                                          "managerApproval": false
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashSessionId").value(7))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void openCashSession_withSellerRole_returnsForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/pos/cash-sessions/open")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappSeller(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"openingAmount\":100.00}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void currentCashSession_whenOpen_returnsSessionSummary() throws Exception {
        when(getCurrentCashSessionUseCase.execute(USER_ID))
                .thenReturn(
                        new CurrentCashSessionResponse(
                                true,
                                7L,
                                new BigDecimal("100.00"),
                                Instant.parse("2026-04-28T14:00:00Z"),
                                new BigDecimal("350.00"),
                                new BigDecimal("100.00"),
                                new BigDecimal("200.00"),
                                new BigDecimal("50.00")));

        mockMvc.perform(
                        get("/api/v1/lojapp/pos/cash-sessions/current")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.cashSessionId").value(7))
                .andExpect(jsonPath("$.expectedAmount").value(350.00))
                .andExpect(jsonPath("$.expectedCardAmount").value(200.00));
    }

    @Test
    void currentCashSession_whenNoneOpen_returnsClosedSnapshot() throws Exception {
        when(getCurrentCashSessionUseCase.execute(USER_ID))
                .thenReturn(
                        new CurrentCashSessionResponse(
                                false,
                                null,
                                null,
                                null,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO));

        mockMvc.perform(
                        get("/api/v1/lojapp/pos/cash-sessions/current")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.cashSessionId").doesNotExist())
                .andExpect(jsonPath("$.expectedAmount").value(0));
    }

    @Test
    void closePreview_whenValidWithCountedAmount_returnsDifferenceAndFlag() throws Exception {
        when(getCashSessionClosePreviewUseCase.execute(eq(USER_ID), eq(7L), eq(new BigDecimal("80.00"))))
                .thenReturn(
                        new CloseCashSessionPreviewResponse(
                                7L,
                                new BigDecimal("100.00"),
                                new BigDecimal("60.00"),
                                new BigDecimal("30.00"),
                                new BigDecimal("10.00"),
                                new BigDecimal("80.00"),
                                new BigDecimal("-20.00"),
                                new BigDecimal("10.00"),
                                true));

        mockMvc.perform(
                        get("/api/v1/lojapp/pos/cash-sessions/7/close-preview")
                                .queryParam("countedAmount", "80.00")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashSessionId").value(7))
                .andExpect(jsonPath("$.differenceAmount").value(-20.00))
                .andExpect(jsonPath("$.managerApprovalRequired").value(true));
    }

    @Test
    void closePreview_withSellerRole_returnsForbidden() throws Exception {
        mockMvc.perform(
                        get("/api/v1/lojapp/pos/cash-sessions/7/close-preview")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappSeller(USER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
