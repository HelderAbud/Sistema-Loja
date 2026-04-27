package com.lojapp.controller;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.MethodSecurityConfig;
import com.lojapp.dto.AuthDtos.UserMeResponse;
import com.lojapp.dto.user.AdminUserSummaryResponse;
import com.lojapp.exception.GlobalExceptionHandler;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.support.TestJwtAuth;
import com.lojapp.service.AuthService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@WebMvcTest(controllers = UserController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private AuthService authService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private static RequestPostProcessor user(long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(TestJwtAuth.userToken(userId));
            return request;
        };
    }

    private static RequestPostProcessor admin(long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(TestJwtAuth.adminToken(userId));
            return request;
        };
    }

    @Test
    void me_withAuthenticatedUser_returnsProfile() throws Exception {
        when(authService.me(88L)).thenReturn(new UserMeResponse(88L, "user@lojapp.test", "USER"));

        mockMvc.perform(get("/api/v1/users/me").with(user(88L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(88))
                .andExpect(jsonPath("$.email").value("user@lojapp.test"))
                .andExpect(jsonPath("$.appRole").value("USER"));
    }

    @Test
    void adminList_withUserRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/admin/list").with(user(91L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminList_withAdminRole_returnsUsers() throws Exception {
        when(authService.listUsersForAdmin(any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(
                                        new AdminUserSummaryResponse(1L, "a@lojapp.test", "ADMIN"),
                                        new AdminUserSummaryResponse(2L, "u@lojapp.test", "USER")),
                                PageRequest.of(0, 20),
                                2));

        mockMvc.perform(get("/api/v1/users/admin/list").with(admin(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("a@lojapp.test"))
                .andExpect(jsonPath("$.content[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.content[1].email").value("u@lojapp.test"))
                .andExpect(jsonPath("$.content[1].role").value("USER"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
