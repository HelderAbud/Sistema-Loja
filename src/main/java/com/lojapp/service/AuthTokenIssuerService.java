package com.lojapp.service;

import com.lojapp.config.JwtProperties;
import com.lojapp.dto.AuthDtos.IssuedAuthTokens;
import com.lojapp.entity.RefreshToken;
import com.lojapp.entity.User;
import com.lojapp.repository.RefreshTokenRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.security.AppRole;
import com.lojapp.security.JwtService;
import com.lojapp.util.TokenHashUtil;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenIssuerService {

    private final UserRepository users;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokens;

    public AuthTokenIssuerService(
            UserRepository users,
            JwtService jwtService,
            JwtProperties jwtProperties,
            RefreshTokenRepository refreshTokens) {
        this.users = users;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokens = refreshTokens;
    }

    public IssuedAuthTokens issueTokens(User user) {
        long userId = Objects.requireNonNull(user.getId(), "user id após persistência");
        String role = AppRole.fromStoredValue(user.getAppRole()).name();
        String access = jwtService.createToken(userId, user.getEmail(), role);
        refreshTokens.deleteByUser_Id(userId);

        String rawRefresh =
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", "");
        RefreshToken row = new RefreshToken();
        row.setUser(users.getReferenceById(userId));
        row.setTokenHash(TokenHashUtil.sha256Hex(rawRefresh));
        row.setExpiresAt(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
        refreshTokens.save(row);
        return new IssuedAuthTokens(access, rawRefresh);
    }
}
