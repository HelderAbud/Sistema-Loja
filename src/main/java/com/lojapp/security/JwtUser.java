package com.lojapp.security;

import java.util.LinkedHashSet;
import java.util.Set;

public record JwtUser(long userId, String email, String appRole) {

    public String authority() {
        return "ROLE_" + AppRole.fromStoredValue(appRole).name();
    }

    public Set<String> authorities() {
        AppRole role = AppRole.fromStoredValue(appRole);
        LinkedHashSet<String> granted = new LinkedHashSet<>();
        granted.add("ROLE_" + role.name());

        // Compatibilidade retroativa enquanto coexistem papéis legados e novos.
        switch (role) {
            case USER -> granted.add("ROLE_CASHIER");
            case ADMIN -> granted.add("ROLE_MANAGER");
            case REPRESENTATIVE -> granted.add("ROLE_SELLER");
            case CASHIER -> granted.add("ROLE_USER");
            case MANAGER -> granted.add("ROLE_ADMIN");
            case SELLER -> granted.add("ROLE_REPRESENTATIVE");
        }
        return granted;
    }
}
