package com.lojapp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.seller.SellerResponse;
import com.lojapp.exception.GlobalExceptionHandler;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.service.contract.CommissionCatalogServiceContract;
import com.lojapp.support.TestJwtAuth;
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

@WebMvcTest(controllers = SellerController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class SellerControllerTest {

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
    void listSellers_withCashierRole_returnsJson() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(catalog.listSellers(USER_ID))
                .thenReturn(List.of(new SellerResponse(1L, "Ana", true, 0, t)));

        mockMvc.perform(
                        get("/api/v1/lojapp/sellers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappCashier(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Ana"));
    }

    @Test
    void createSeller_blankName_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/sellers")
                                .contentType(APPLICATION_JSON)
                                .content("{\"displayName\":\"\"}")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_ERROR.code()));
    }

    @Test
    void createSeller_withCashierRole_returnsForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/sellers")
                                .contentType(APPLICATION_JSON)
                                .content("{\"displayName\":\"Ana\"}")
                                .with(lojappCashier(USER_ID)))
                .andExpect(status().isForbidden());
    }
}
