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
import com.lojapp.dto.hierarchy.ProductModelResponse;
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

@WebMvcTest(controllers = ProductModelController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ProductModelControllerTest {

    private static final long USER_ID = 90L;

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
    void listModels_withoutCollectionFilter_delegatesToService() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(hierarchy.listModels(USER_ID, 4L, null))
                .thenReturn(
                        List.of(
                                new ProductModelResponse(
                                        1L, 4L, "Marca", null, null, "Modelo A", t, t)));

        mockMvc.perform(
                        get("/api/v1/lojapp/product-models")
                                .param("brandId", "4")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Modelo A"));
    }

    @Test
    void listModels_withCollectionId_delegatesToService() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(hierarchy.listModels(USER_ID, 4L, 9L))
                .thenReturn(
                        List.of(
                                new ProductModelResponse(2L, 4L, "Marca", 9L, "Col", "Modelo B", t, t)));

        mockMvc.perform(
                        get("/api/v1/lojapp/product-models")
                                .param("brandId", "4")
                                .param("collectionId", "9")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].collectionId").value(9));
    }

    @Test
    void createModel_blankName_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/product-models")
                                .contentType(APPLICATION_JSON)
                                .content("{\"brandId\":1,\"name\":\"\"}")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_ERROR.code()));
    }

    @Test
    void getModel_delegatesToService() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(hierarchy.getModel(eq(USER_ID), eq(12L)))
                .thenReturn(
                        new ProductModelResponse(12L, 1L, "M", null, null, "Mod", t, t));

        mockMvc.perform(
                        get("/api/v1/lojapp/product-models/12")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));
    }
}
