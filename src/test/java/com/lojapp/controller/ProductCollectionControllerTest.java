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
import com.lojapp.dto.hierarchy.ProductCollectionResponse;
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

@WebMvcTest(controllers = ProductCollectionController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ProductCollectionControllerTest {

    private static final long USER_ID = 89L;

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
    void listCollections_returnsJson() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(hierarchy.listCollections(USER_ID, 3L))
                .thenReturn(List.of(new ProductCollectionResponse(1L, 3L, "Marca", "VerÃƒÂ£o", t, t)));

        mockMvc.perform(
                        get("/api/v1/lojapp/product-collections")
                                .param("brandId", "3")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("VerÃƒÂ£o"))
                .andExpect(jsonPath("$[0].brandId").value(3));
    }

    @Test
    void createCollection_blankName_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/lojapp/product-collections")
                                .contentType(APPLICATION_JSON)
                                .content("{\"brandId\":1,\"name\":\"\"}")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_ERROR.code()));
    }

    @Test
    void getCollection_delegatesToService() throws Exception {
        Instant t = Instant.parse("2026-02-01T10:00:00Z");
        when(hierarchy.getCollection(eq(USER_ID), eq(7L)))
                .thenReturn(new ProductCollectionResponse(7L, 2L, "B", "Col", t, t));

        mockMvc.perform(
                        get("/api/v1/lojapp/product-collections/7")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Col"));
    }
}
