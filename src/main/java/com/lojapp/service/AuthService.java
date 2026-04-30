package com.lojapp.service;

import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.dto.AuthDtos.UserMeResponse;
import com.lojapp.dto.user.AdminUserSummaryResponse;
import com.lojapp.service.contract.AuthServiceContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements AuthServiceContract {

    private final AuthRegisterUseCase authRegisterUseCase;
    private final AuthLoginUseCase authLoginUseCase;
    private final AuthRefreshUseCase authRefreshUseCase;
    private final AuthLogoutUseCase authLogoutUseCase;
    private final AuthMeUseCase authMeUseCase;
    private final AuthListUsersForAdminUseCase authListUsersForAdminUseCase;

    public AuthService(
            AuthRegisterUseCase authRegisterUseCase,
            AuthLoginUseCase authLoginUseCase,
            AuthRefreshUseCase authRefreshUseCase,
            AuthLogoutUseCase authLogoutUseCase,
            AuthMeUseCase authMeUseCase,
            AuthListUsersForAdminUseCase authListUsersForAdminUseCase) {
        this.authRegisterUseCase = authRegisterUseCase;
        this.authLoginUseCase = authLoginUseCase;
        this.authRefreshUseCase = authRefreshUseCase;
        this.authLogoutUseCase = authLogoutUseCase;
        this.authMeUseCase = authMeUseCase;
        this.authListUsersForAdminUseCase = authListUsersForAdminUseCase;
    }

    public IssuedAuthTokens register(RegisterRequest req) {
        return authRegisterUseCase.execute(req);
    }

    public IssuedAuthTokens login(LoginRequest req) {
        return authLoginUseCase.execute(req);
    }

    public IssuedAuthTokens refresh(String rawRefresh) {
        return authRefreshUseCase.execute(rawRefresh);
    }

    public void logout(String rawRefresh) {
        authLogoutUseCase.execute(rawRefresh);
    }

    public UserMeResponse me(long userId) {
        return authMeUseCase.execute(userId);
    }

    public Page<AdminUserSummaryResponse> listUsersForAdmin(Pageable pageable) {
        return authListUsersForAdminUseCase.execute(pageable);
    }
}
