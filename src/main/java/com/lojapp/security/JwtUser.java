package com.lojapp.security;

public record JwtUser(long userId, String email, String appRole) {

    public String authority() {
        return "ROLE_" + AppRole.fromStoredValue(appRole).name();
    }
}
