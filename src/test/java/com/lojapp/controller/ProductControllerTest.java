package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.config.SecurityConfig;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.dto.product.ProductResponse;
import com.lojapp.exception.GlobalExceptionHandler;
import com.lojapp.exception.domain.BrandNotFoundException;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.AuthCsrfGuardFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.support.TestJwtAuth;
import com.lojapp.service.contract.LojappCatalogServiceContract;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
class ProductControllerTest {

    private static final long USER_ID = 42L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;
    @MockBean private AuthCsrfGuardFilter authCsrfGuardFilter;

    @MockBean private LojappCatalogServiceContract catalog;

    @BeforeEach
    void jwtFilterPassesThrough() throws Exception {
        doAnswer(
                        invocation -> {
                            HttpServletRequest req = invocation.getArgument(0);
                            HttpServletResponse res = invocation.getArgument(1);
                            FilterChain chain = invocation.getArgument(2);
                            chain.doFilter(req, res);
                            return null;
                        })
                .when(jwtAuthFilter)
                .doFilter(any(), any(), any());
        doAnswer(
                        invocation -> {
                            HttpServletRequest req = invocation.getArgument(0);
                            HttpServletResponse res = invocation.getArgument(1);
                            FilterChain chain = invocation.getArgument(2);
                            chain.doFilter(req, res);
                            return null;
                        })
                .when(authRateLimitFilter)
                .doFilter(any(), any(), any());
        doAnswer(
                        invocation -> {
                            HttpServletRequest req = invocation.getArgument(0);
                            HttpServletResponse res = invocation.getArgument(1);
                            FilterChain chain = invocation.getArgument(2);
                            chain.doFilter(req, res);
                            return null;
                        })
                .when(authCsrfGuardFilter)
                .doFilter(any(), any(), any());
    }

    @Test
    void listProducts_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/lojapp/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void listProducts_withAuthentication_returnsPagedEnvelope() throws Exception {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        ProductResponse item =
                new ProductResponse(
                        1L,
                        "P1",
                        "Marca",
                        null,
                        null,
                        null,
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        now,
                        now,
                        null,
                        null,
                        null,
                        null);
        var page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        when(catalog.searchProducts(eq(USER_ID), isNull(), isNull(), eq(false), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/v1/lojapp/products")
                                .with(
                                        authentication(TestJwtAuth.userToken(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("P1"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void listProducts_forwardsPaginationAndFiltersToService() throws Exception {
        when(catalog.searchProducts(
                        eq(USER_ID), eq(5L), eq("caneta"), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(
                        get("/api/v1/lojapp/products")
                                .param("brandId", "5")
                                .param("q", "caneta")
                                .param("lowStock", "true")
                                .param("page", "1")
                                .param("size", "10")
                                .with(
                                        authentication(TestJwtAuth.userToken(USER_ID))))
                .andExpect(status().isOk());

        verify(catalog)
                .searchProducts(
                        eq(USER_ID),
                        eq(5L),
                        eq("caneta"),
                        eq(true),
                        argThat(
                                p ->
                                        p.getPageNumber() == 1
                                                && p.getPageSize() == 10));
    }

    @Test
    void createProduct_blankName_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/products")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"","brandId":null,"ean":null,"ncm":null,"sku":null,"costPrice":1,"salePrice":1,"minimumStock":0}
                                        """)
                                .with(
                                        authentication(TestJwtAuth.userToken(USER_ID))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_ERROR.code()));
    }

    @Test
    void createProduct_nullCostPrice_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/products")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"X","brandId":null,"ean":null,"ncm":null,"sku":null,"costPrice":null,"salePrice":1,"minimumStock":0}
                                        """)
                                .with(
                                        authentication(TestJwtAuth.userToken(USER_ID))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_ERROR.code()));
    }

    @Test
    void createProduct_unknownBrand_returns404() throws Exception {
        when(catalog.createProduct(eq(USER_ID), any(ProductRequest.class)))
                .thenThrow(new BrandNotFoundException());

        mockMvc.perform(
                        post("/api/v1/lojapp/products")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"P","brandId":1,"ean":null,"ncm":null,"sku":null,"costPrice":1,"salePrice":2,"minimumStock":0}
                                        """)
                                .with(
                                        authentication(TestJwtAuth.userToken(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_FOUND.code()));
    }
}
