package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.application.sale.CreatePosSaleUseCase;
import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.sale.PosSaleFinalizeResponse;
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

@WebMvcTest(controllers = PosSaleController.class)
@Import(MethodSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class PosSaleControllerTest {

    private static final long USER_ID = 42L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private CreatePosSaleUseCase createPosSaleUseCase;

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

    @Test
    void finalizeSale_whenValid_returnsOk() throws Exception {
        when(createPosSaleUseCase.execute(eq(USER_ID), any(), any()))
                .thenReturn(
                        new PosSaleFinalizeResponse(
                                11L, 7L, new BigDecimal("100.00"), Instant.parse("2026-04-28T15:00:00Z")));

        mockMvc.perform(
                        post("/api/v1/lojapp/pos/sales/finalize")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .header("Idempotency-Key", "abc-123")
                                .with(lojappUser(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "cashSessionId": 7,
                                          "productId": 3,
                                          "quantity": 2.0,
                                          "unitPrice": 50.00,
                                          "unitCost": 30.00,
                                          "payments": [
                                            {"paymentMethod": "CARD", "amount": 100.00}
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleId").value(11))
                .andExpect(jsonPath("$.cashSessionId").value(7))
                .andExpect(jsonPath("$.totalAmount").value(100.00));
    }
}
