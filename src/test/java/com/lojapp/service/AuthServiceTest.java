package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.RefreshTokenRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired private AuthService authService;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private MeterRegistry meterRegistry;

    @Test
    void register_thenLogin_returnsTokens() {
        var reg = new RegisterRequest("auth-flow@lojapp.test", "senha1234", null);
        IssuedAuthTokens t1 = authService.register(reg);
        assertThat(t1.accessToken()).isNotBlank();
        assertThat(t1.rawRefreshToken()).isNotBlank();

        IssuedAuthTokens t2 = authService.login(new LoginRequest(reg.email(), reg.password()));
        assertThat(t2.accessToken()).isNotBlank();
        assertThat(t2.rawRefreshToken()).isNotBlank();
    }

    @Test
    void refresh_rotatesTokens() {
        var reg = new RegisterRequest("refresh-flow@lojapp.test", "senha1234", null);
        IssuedAuthTokens first = authService.register(reg);
        IssuedAuthTokens second = authService.refresh(first.rawRefreshToken());
        assertThat(second.accessToken()).isNotBlank();
        assertThat(second.rawRefreshToken()).isNotBlank();
        assertThat(second.rawRefreshToken()).isNotEqualTo(first.rawRefreshToken());
    }

    @Test
    void logout_revokesRefreshToken() {
        var reg = new RegisterRequest("logout-flow@lojapp.test", "senha1234", null);
        IssuedAuthTokens issued = authService.register(reg);

        authService.logout(issued.rawRefreshToken());

        assertThat(refreshTokens.findByTokenHash(com.lojapp.util.TokenHashUtil.sha256Hex(issued.rawRefreshToken())))
                .isEmpty();
        assertThatThrownBy(() -> authService.refresh(issued.rawRefreshToken()))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.UNAUTHORIZED));
    }

    @Test
    void register_duplicateEmail_returns409() {
        authService.register(new RegisterRequest("dup@lojapp.test", "senha1234", null));
        assertThatThrownBy(
                        () ->
                                authService.register(
                                        new RegisterRequest("dup@lojapp.test", "outrasenha", null)))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.CONFLICT));
    }

    @Test
    void login_wrongPassword_returns401() {
        authService.register(new RegisterRequest("login-wrong@lojapp.test", "senha1234", null));
        assertThatThrownBy(
                        () ->
                                authService.login(
                                        new LoginRequest("login-wrong@lojapp.test", "outrasenha")))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.UNAUTHORIZED));
    }

    @Test
    void login_unknownEmail_returns401() {
        assertThatThrownBy(
                        () ->
                                authService.login(
                                        new LoginRequest("inexistente@lojapp.test", "senha1234")))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.UNAUTHORIZED));
    }

    @Test
    void me_unknownUser_returns404() {
        assertThatThrownBy(() -> authService.me(999_999_999L))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.NOT_FOUND));
    }

    @Test
    void refresh_invalidToken_incrementsInvalidMetric() {
        double before = meterRegistry.counter("lojapp.auth.refresh", "outcome", "invalid").count();
        assertThatThrownBy(() -> authService.refresh("token-opaco-inexistente"))
                .isInstanceOf(LojappDomainException.class);
        assertThat(meterRegistry.counter("lojapp.auth.refresh", "outcome", "invalid").count())
                .isEqualTo(before + 1.0);
    }

    @Test
    void refresh_success_incrementsSuccessMetric() {
        var reg = new RegisterRequest("metrics-ok@lojapp.test", "senha1234", null);
        IssuedAuthTokens first = authService.register(reg);
        double before = meterRegistry.counter("lojapp.auth.refresh", "outcome", "success").count();
        authService.refresh(first.rawRefreshToken());
        assertThat(meterRegistry.counter("lojapp.auth.refresh", "outcome", "success").count())
                .isEqualTo(before + 1.0);
    }
}
