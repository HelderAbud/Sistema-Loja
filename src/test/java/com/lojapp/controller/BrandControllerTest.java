package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.brand.BrandRequest;
import com.lojapp.dto.brand.BrandResponse;
import com.lojapp.exception.GlobalExceptionHandler;
import com.lojapp.exception.domain.BrandNotFoundException;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.support.TestJwtAuth;
import com.lojapp.service.LojappCatalogService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = BrandController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class BrandControllerTest {

    private static final long USER_ID = 77L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private LojappCatalogService catalog;

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
    void createBrand_blankName_returnsStructuredApiError() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/brands")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"\"}")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void listBrands_withAuthentication_returnsJson() throws Exception {
        when(catalog.listBrands(USER_ID)).thenReturn(List.of(new BrandResponse(1L, "Marca A")));

        mockMvc.perform(
                        get("/api/v1/lojapp/brands")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Marca A"));
    }

    @Test
    void updateBrand_returnsJson() throws Exception {
        when(catalog.updateBrand(eq(USER_ID), eq(3L), any(BrandRequest.class)))
                .thenReturn(new BrandResponse(3L, "Marca Renomeada"));

        mockMvc.perform(
                        put("/api/v1/lojapp/brands/3")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"Marca Renomeada\"}")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Marca Renomeada"));
    }

    @Test
    void updateBrand_notFound_returns404() throws Exception {
        when(catalog.updateBrand(eq(USER_ID), eq(99L), any(BrandRequest.class)))
                .thenThrow(new BrandNotFoundException());

        mockMvc.perform(
                        put("/api/v1/lojapp/brands/99")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"X\"}")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_FOUND.code()));
    }

    @Test
    void deleteBrand_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/lojapp/brands/2").with(lojappUser(USER_ID)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBrand_notFound_returns404() throws Exception {
        doThrow(new BrandNotFoundException()).when(catalog).deleteBrand(USER_ID, 404L);

        mockMvc.perform(delete("/api/v1/lojapp/brands/404").with(lojappUser(USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_FOUND.code()));
    }
}
