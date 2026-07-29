package com.lojapp.security;

import java.util.Set;

public record JwtUser(long userId, String email, String appRole) {

    public String authority() {
        return "ROLE_" + AppRole.fromStoredValue(appRole).name();
    }

    /** Authorities exactas do papel persistido — sem aliases nem elevação de privilégio. */
    public Set<String> authorities() {
        return Set.of(authority());
    }
}
