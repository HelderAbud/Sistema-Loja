package com.lojapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lojapp.config.AuthRefreshCookieSupport;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.exception.GlobalExceptionHandler;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtAuthFilter jwtAuthFilter;

    @MockBean private AuthRateLimitFilter authRateLimitFilter;

    @MockBean private AuthService authService;

    @MockBean private AuthRefreshCookieSupport refreshCookie;

    @Test
    void register_returnsAccessTokenAndSetsRefreshCookie() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new IssuedAuthTokens("mock.jwt.access", "mock-refresh-secret"));

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"api@lojapp.test","password":"senha1234"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.access"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        verify(refreshCookie).writeRefreshCookie(any(), anyString());
    }

    @Test
    void login_returnsAccessToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new IssuedAuthTokens("mock.login.access", "mock-refresh-2"));

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"api@lojapp.test","password":"senha1234"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.login.access"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void register_invalidEmail_returnsStructuredValidationError() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"nao-e-email","password":"senha1234"}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_ERROR.code()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void register_passwordTooShort_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"ok@lojapp.test","password":"curta"}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_ERROR.code()));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new LojappDomainException(ApiErrorCode.CONFLICT, "Email jÃ¡ cadastrado"));

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"dup@lojapp.test","password":"senha1234"}
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.CONFLICT.code()))
                .andExpect(jsonPath("$.message").value("Email jÃ¡ cadastrado"));
    }

    @Test
    void register_whenDisabled_returns403() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(
                        new LojappDomainException(
                                ApiErrorCode.FORBIDDEN, "O registo pÃºblico estÃ¡ desativado."));

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"x@lojapp.test","password":"senha1234"}
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.FORBIDDEN.code()))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new LojappDomainException(ApiErrorCode.UNAUTHORIZED, "Credenciais invÃ¡lidas"));

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"x@lojapp.test","password":"senha1234"}
                                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.UNAUTHORIZED.code()))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void refresh_missingToken_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.BAD_REQUEST.code()))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"))
                .andExpect(jsonPath("$.message").value("Refresh token ausente"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void refresh_readsTokenFromCookieWhenBodyIsEmpty() throws Exception {
        when(refreshCookie.cookieName()).thenReturn("lojapp_rt");
        when(authService.refresh("cookie-refresh-token"))
                .thenReturn(new IssuedAuthTokens("mock.refresh.access", "new-cookie-refresh"));

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content("{}")
                                .cookie(new jakarta.servlet.http.Cookie("lojapp_rt", "cookie-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.refresh.access"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void refresh_conflictingBodyAndCookie_returns401() throws Exception {
        when(refreshCookie.cookieName()).thenReturn("lojapp_rt");

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content("{\"refreshToken\":\"body-token\"}")
                                .cookie(new jakarta.servlet.http.Cookie("lojapp_rt", "cookie-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.UNAUTHORIZED.code()))
                .andExpect(jsonPath("$.message").value("Refresh token inconsistente"));

        verifyNoInteractions(authService);
    }

    @Test
    void logout_returns204AndClearsRefreshCookie() throws Exception {
        when(refreshCookie.cookieName()).thenReturn("lojapp_rt");
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());

        verify(refreshCookie).clearRefreshCookie(any());
    }

    @Test
    void logout_withRefreshCookie_revokesSessionAndClearsCookie() throws Exception {
        when(refreshCookie.cookieName()).thenReturn("lojapp_rt");

        mockMvc.perform(post("/api/v1/auth/logout").cookie(new jakarta.servlet.http.Cookie("lojapp_rt", "rt-1")))
                .andExpect(status().isNoContent());

        verify(authService).logout("rt-1");
        verify(refreshCookie).clearRefreshCookie(any());
    }

    @Test
    void login_unexpectedError_returns500WithStructuredContract() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"x@lojapp.test","password":"senha1234"}
                                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.INTERNAL_ERROR.code()))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
