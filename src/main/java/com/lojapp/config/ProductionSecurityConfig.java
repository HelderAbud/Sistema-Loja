package com.lojapp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Em {@code prod}, o segredo JWT não pode ser o placeholder de desenvolvimento nem vazio.
 */
@Configuration
@Profile("prod")
public class ProductionSecurityConfig {

    private static final String FORBIDDEN_DEV_MARKER = "dev-only-change-this-secret";
    private static final int MIN_SECRET_LENGTH = 32;

    /** Texto comum: ambos os ramos devem mencionar explicitamente LOJAPP_JWT_SECRET (também para pesquisa em logs/testes). */
    private static final String JWT_SECRET_ACTION =
            "Defina LOJAPP_JWT_SECRET (ou lojapp.jwt.secret) com valor forte e aleatório (mín. 32 bytes).";

    private final JwtProperties jwtProperties;

    public ProductionSecurityConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void requireProductionJwtSecret() {
        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Perfil prod: segredo JWT vazio ou em falta. " + JWT_SECRET_ACTION);
        }
        if (secret.contains(FORBIDDEN_DEV_MARKER)) {
            throw new IllegalStateException(
                    "Perfil prod: não use o segredo JWT por omissão de desenvolvimento. " + JWT_SECRET_ACTION);
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "Perfil prod: segredo JWT demasiado curto (< 32 caracteres). " + JWT_SECRET_ACTION);
        }
    }
}
