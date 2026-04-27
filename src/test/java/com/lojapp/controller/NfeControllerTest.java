package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.nfe.NfeApplySuggestionsResponse;
import com.lojapp.exception.GlobalExceptionHandler;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.application.nfe.ApplyNfeImportSuggestionsUseCase;
import com.lojapp.application.nfe.ImportNfeUseCase;
import com.lojapp.support.TestJwtAuth;
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

@WebMvcTest(controllers = NfeController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class NfeControllerTest {

    private static final long USER_ID = 91L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private ImportNfeUseCase importNfeUseCase;

    @MockBean private ApplyNfeImportSuggestionsUseCase applyNfeImportSuggestionsUseCase;

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
    void applySuggestions_withoutBody_delegatesToService() throws Exception {
        when(applyNfeImportSuggestionsUseCase.execute(eq(USER_ID), eq(5L), isNull()))
                .thenReturn(
                        new NfeApplySuggestionsResponse(2, 1, 1, 0, 10L, "Marca", 20L));

        mockMvc.perform(
                        post("/api/v1/lojapp/nfe/entries/5/apply-suggestions")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nfeLineCount").value(2))
                .andExpect(jsonPath("$.brandAssignedCount").value(1))
                .andExpect(jsonPath("$.supplierAssignedCount").value(1))
                .andExpect(jsonPath("$.appliedBrandId").value(10))
                .andExpect(jsonPath("$.supplierIdFromEntry").value(20));
    }

    @Test
    void applySuggestions_withBody_delegatesToService() throws Exception {
        when(applyNfeImportSuggestionsUseCase.execute(eq(USER_ID), eq(6L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        new NfeApplySuggestionsResponse(1, 0, 0, 0, null, null, null));

        mockMvc.perform(
                        post("/api/v1/lojapp/nfe/entries/6/apply-suggestions")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"setBrandOnImportedProducts\":false,\"setSupplierOnImportedProducts\":false}")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                                .with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandAssignedCount").value(0));
    }
}
