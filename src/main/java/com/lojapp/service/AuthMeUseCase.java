package com.lojapp.service;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.AuthDtos.UserMeResponse;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.UserRepository;
import com.lojapp.security.AppRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthMeUseCase {

    private final UserRepository users;

    public AuthMeUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserMeResponse execute(long userId) {
        var user =
                users.findById(userId)
                        .orElseThrow(
                                () ->
                                        new LojappDomainException(
                                                ApiErrorCode.NOT_FOUND, "Utilizador não encontrado"));
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                AppRole.fromStoredValue(user.getAppRole()).name());
    }
}
