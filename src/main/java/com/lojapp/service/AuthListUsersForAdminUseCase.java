package com.lojapp.service;

import com.lojapp.dto.user.AdminUserSummaryResponse;
import com.lojapp.entity.User;
import com.lojapp.repository.UserRepository;
import com.lojapp.security.AppRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthListUsersForAdminUseCase {

    private final UserRepository users;

    public AuthListUsersForAdminUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> execute(Pageable pageable) {
        return users.findAll(pageable).map(this::toAdminSummary);
    }

    private AdminUserSummaryResponse toAdminSummary(User u) {
        return new AdminUserSummaryResponse(
                u.getId(),
                u.getEmail(),
                AppRole.fromStoredValue(u.getAppRole()).name());
    }
}
