package com.lojapp.application.contract;

import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.dto.AuthDtos.UserMeResponse;
import com.lojapp.dto.user.AdminUserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Contrato de aplicação para autenticação/sessão (alinhado ao padrão NFe/use-case contracts). */
public interface AuthServiceContract {

    IssuedAuthTokens register(RegisterRequest req);

    IssuedAuthTokens login(LoginRequest req);

    IssuedAuthTokens refresh(String rawRefresh);

    void logout(String rawRefresh);

    UserMeResponse me(long userId);

    Page<AdminUserSummaryResponse> listUsersForAdmin(Pageable pageable);
}
