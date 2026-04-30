package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.dashboard.BrandDashboardResponse;
import com.lojapp.support.TestJwtAuth;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.service.contract.DashboardServiceContract;
import com.lojapp.service.contract.InventoryServiceContract;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = DashboardController.class)
@Import(MethodSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    private static final long USER_ID = 77L;

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private DashboardServiceContract dashboard;

    @MockBean private InventoryServiceContract inventory;

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
    void dashboardByBrand_delegatesWithDefaultWindow() throws Exception {
        when(dashboard.brandDashboard(eq(USER_ID), any(), any(), eq(50), eq(0)))
                .thenAnswer(
                        inv -> {
                            java.time.Instant from = inv.getArgument(1);
                            java.time.Instant to = inv.getArgument(2);
                            return new BrandDashboardResponse(from, to, List.of(), 0, 50, 0);
                        });

        mockMvc.perform(get("/api/v1/lojapp/dashboard/brands").with(lojappUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics").isArray());
    }
}
