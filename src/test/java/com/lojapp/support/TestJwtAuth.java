package com.lojapp.support;

import com.lojapp.security.JwtUser;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Autenticação de teste com o mesmo tipo de principal que a API em produção ({@link JwtUser}). */
public final class TestJwtAuth {

    private TestJwtAuth() {}

    public static UsernamePasswordAuthenticationToken userToken(long userId) {
        return token(userId, userId + "@unit.test", "USER");
    }

    public static UsernamePasswordAuthenticationToken adminToken(long userId) {
        return token(userId, userId + "@admin.unit.test", "ADMIN");
    }

    public static UsernamePasswordAuthenticationToken sellerToken(long userId) {
        return token(userId, userId + "@seller.unit.test", "SELLER");
    }

    public static UsernamePasswordAuthenticationToken managerToken(long userId) {
        return token(userId, userId + "@manager.unit.test", "MANAGER");
    }

    public static UsernamePasswordAuthenticationToken cashierToken(long userId) {
        return token(userId, userId + "@cashier.unit.test", "CASHIER");
    }

    private static UsernamePasswordAuthenticationToken token(long userId, String email, String role) {
        JwtUser principal = new JwtUser(userId, email, role);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(principal.authority())));
    }
}
