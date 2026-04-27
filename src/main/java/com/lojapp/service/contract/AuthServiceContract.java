package com.lojapp.service.contract;

import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.dto.AuthDtos.UserMeResponse;
import com.lojapp.dto.user.AdminUserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthServiceContract {

    IssuedAuthTokens register(RegisterRequest req);

    IssuedAuthTokens login(LoginRequest req);

    IssuedAuthTokens refresh(String rawRefresh);

    void logout(String rawRefresh);

    UserMeResponse me(long userId);

    Page<AdminUserSummaryResponse> listUsersForAdmin(Pageable pageable);
}
