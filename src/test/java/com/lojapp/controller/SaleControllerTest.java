package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.dto.sale.SaleCreatedResponse;
import com.lojapp.dto.sale.SaleListItemResponse;
import com.lojapp.dto.sale.SalePageResponse;
import com.lojapp.dto.sale.SalesDailyPointResponse;
import com.lojapp.dto.sale.SalesSummaryResponse;
import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.support.TestJwtAuth;
import com.lojapp.service.contract.SalesServiceContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    @Test
    void summarizeSales_withAuthentication_returnsSummary() throws Exception {
        when(sales.summarizeSales(eq(USER_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(
                        new SalesSummaryResponse(
                                new BigDecimal("150.00"),
                                new BigDecimal("10.000"),
                                new BigDecimal("15.00")));

        mockMvc.perform(
                        get("/api/v1/lojapp/sales/summary")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(150.0))
                .andExpect(jsonPath("$.unitsSold").value(10.0))
                .andExpect(jsonPath("$.averageTicket").value(15.0));
    }

    @Test
    void summarizeSalesDaily_withAuthentication_returnsArray() throws Exception {
        when(sales.summarizeSalesDaily(eq(USER_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(
                        List.of(
                                new SalesDailyPointResponse(
                                        LocalDate.parse("2026-04-01"),
                                        new BigDecimal("50.00"),
                                        new BigDecimal("3.000"))));

        mockMvc.perform(
                        get("/api/v1/lojapp/sales/daily")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-04-01"))
                .andExpect(jsonPath("$[0].revenue").value(50.0))
                .andExpect(jsonPath("$[0].unitsSold").value(3.0));
    }

    @Test
    void registerSale_withAuthentication_forwardsIdempotencyKey() throws Exception {
        Instant soldAt = Instant.parse("2026-04-01T12:00:00Z");
        when(sales.registerSale(eq(USER_ID), any(), eq(Optional.of("idem-key-1"))))
                .thenReturn(
                        new SaleCreatedResponse(
                                100L,
                                3L,
                                new BigDecimal("1.000"),
                                new BigDecimal("9.90"),
                                new BigDecimal("5.00"),
                                soldAt));

        mockMvc.perform(
                        post("/api/v1/lojapp/sales")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .header("Idempotency-Key", "idem-key-1")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"productId":3,"quantity":"1.000","unitPrice":"9.90","unitCost":"5.00"}\
                                        """)
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.productId").value(3));

        verify(sales).registerSale(eq(USER_ID), any(), eq(Optional.of("idem-key-1")));
    }

    @Test
    void cancelSale_withAuthentication_invokesService() throws Exception {
        doNothing().when(sales).cancelSale(eq(USER_ID), eq(77L));

        mockMvc.perform(
                        post("/api/v1/lojapp/sales/77/cancel")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk());

        verify(sales).cancelSale(eq(USER_ID), eq(77L));
    }
}
