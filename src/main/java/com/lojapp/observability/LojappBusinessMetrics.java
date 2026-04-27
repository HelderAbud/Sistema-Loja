package com.lojapp.observability;

import com.lojapp.application.idempotency.ApiIdempotencyScope;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class LojappBusinessMetrics {

    private final MeterRegistry registry;

    public LojappBusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordSaleRegistered() {
        registry.counter("lojapp.sales.registered", "outcome", "success").increment();
    }

    public void recordIdempotencyReplay(ApiIdempotencyScope scope) {
        registry
                .counter("lojapp.idempotency.replay", "scope", scope.name())
                .increment();
    }

    public void recordNfeImportSuccess() {
        registry.counter("lojapp.nfe.imports", "outcome", "success").increment();
    }

    public void recordNfeImportDuplicateKey() {
        registry.counter("lojapp.nfe.imports", "outcome", "duplicate_key").increment();
    }

    public void recordNfeImportDuplicateXml() {
        registry.counter("lojapp.nfe.imports", "outcome", "duplicate_xml").increment();
    }
}
