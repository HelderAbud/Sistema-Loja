package com.lojapp.support;

import com.lojapp.security.JwtUser;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Autenticação de teste com o mesmo tipo de principal que a API em produção ({@link JwtUser}). */
public final class TestJwtAuth {

    private TestJwtAuth() {}

    public static UsernamePasswordAuthenticationToken userToken(long userId) {
        JwtUser principal = new JwtUser(userId, userId + "@unit.test", "USER");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_CASHIER")));
    }

    public static UsernamePasswordAuthenticationToken adminToken(long userId) {
        JwtUser principal = new JwtUser(userId, userId + "@admin.unit.test", "ADMIN");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_MANAGER")));
    }

    public static UsernamePasswordAuthenticationToken sellerToken(long userId) {
        JwtUser principal = new JwtUser(userId, userId + "@seller.unit.test", "SELLER");
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_SELLER")));
    }
}
