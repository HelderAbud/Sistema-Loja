package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.dto.sale.SaleListItemResponse;
import com.lojapp.dto.sale.SalePageResponse;
import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.support.TestJwtAuth;
import com.lojapp.service.contract.SalesServiceContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = SaleController.class)
@Import(MethodSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class SaleControllerTest {

    private static final long USER_ID = 42L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private SalesServiceContract sales;

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
    void listSales_withAuthentication_returnsPagedEnvelope() throws Exception {
        Instant soldAt = Instant.parse("2026-04-01T12:00:00Z");
        var row =
                new SaleListItemResponse(
                        9L,
                        3L,
                        "Produto A",
                        "Marca B",
                        new BigDecimal("2"),
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        soldAt,
                        false);
        var page =
                new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1);
        when(sales.listSales(eq(USER_ID), any(), any(), isNull(), isNull(), any()))
                .thenReturn(SalePageResponse.from(page));

        mockMvc.perform(
                        get("/api/v1/lojapp/sales")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productName").value("Produto A"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
