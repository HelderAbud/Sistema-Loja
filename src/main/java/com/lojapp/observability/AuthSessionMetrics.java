package com.lojapp.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Métricas de sessão / refresh para deteção de abuso e dashboards (Prometheus). */
@Component
public class AuthSessionMetrics {

    private final MeterRegistry registry;

    public AuthSessionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Resultado de {@code POST /api/v1/auth/refresh}.
     *
     * @param outcome success | expired | invalid
     */
    public void recordRefreshOutcome(String outcome) {
        registry.counter("lojapp.auth.refresh", "outcome", outcome).increment();
    }
}
