package com.lojapp.service;

import com.lojapp.dto.user.AdminUserSummaryResponse;
import com.lojapp.entity.User;
import com.lojapp.repository.UserRepository;
import com.lojapp.security.AppRole;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    public Page<AdminUserSummaryResponse> execute(long actorUserId, Pageable pageable) {
        return users.findById(actorUserId)
                .map(
                        u ->
                                (Page<AdminUserSummaryResponse>)
                                        new PageImpl<>(List.of(toAdminSummary(u)), pageable, 1))
                .orElseGet(() -> Page.empty(pageable));
    }

    private AdminUserSummaryResponse toAdminSummary(User u) {
        return new AdminUserSummaryResponse(
                u.getId(),
                u.getEmail(),
                AppRole.fromStoredValue(u.getAppRole()).name());
    }
}
