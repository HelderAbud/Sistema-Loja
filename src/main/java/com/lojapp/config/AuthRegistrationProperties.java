package com.lojapp.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Política de registo público: desligar totalmente, restringir domínios de email, convite opcional e
 * taxa por IP (esta última aplicada em {@link com.lojapp.security.AuthRateLimitFilter}).
 */
@ConfigurationProperties(prefix = "lojapp.auth.registration")
public record AuthRegistrationProperties(
        boolean enabled,
        /** CSV de domínios permitidos (parte após {@code @}), vazio = qualquer. */
        String allowedDomains,
        /** Máximo de pedidos POST /register por endereço IP por hora. */
        int maxPerIpPerHour,
        /**
         * Se não vazio, {@code POST /register} exige {@code inviteToken} no corpo igual a este valor
         * (comparação via hash; configurar com segredo forte em produção).
         */
        String inviteSecret) {

    public AuthRegistrationProperties {
        maxPerIpPerHour = Math.max(1, maxPerIpPerHour);
    }

    public boolean inviteSecretConfigured() {
        return inviteSecret != null && !inviteSecret.isBlank();
    }

    /** Conjunto normalizado (minúsculas) para lookup; vazio = sem restrição de domínio. */
    public Set<String> allowedDomainSet() {
        if (allowedDomains == null || allowedDomains.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(allowedDomains.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }
}
