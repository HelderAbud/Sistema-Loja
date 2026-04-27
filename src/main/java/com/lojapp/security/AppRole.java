package com.lojapp.security;

import java.util.Locale;

public enum AppRole {
    USER,
    ADMIN,
    /** B2B: representante comercial (mesmo acesso operacional base que USER nesta versão). */
    REPRESENTATIVE;

    public static AppRole fromStoredValue(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return USER;
        }
        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ADMIN", "ROLE_ADMIN", "LOJA_ADMIN" -> ADMIN;
            case "USER", "ROLE_USER", "LOJA_USER" -> USER;
            case "REPRESENTATIVE",
                    "ROLE_REPRESENTATIVE",
                    "LOJA_REPRESENTATIVE",
                    "REP" -> REPRESENTATIVE;
            default -> USER;
        };
    }
}
