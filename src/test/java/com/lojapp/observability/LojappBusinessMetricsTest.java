package com.lojapp.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.application.idempotency.ApiIdempotencyScope;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LojappBusinessMetricsTest {

    private SimpleMeterRegistry registry;
    private LojappBusinessMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new LojappBusinessMetrics(registry);
    }

    @Test
    void recordSaleRegistered_incrementsCounter() {
        metrics.recordSaleRegistered();
        assertThat(registry.get("lojapp.sales.registered").counter().count()).isEqualTo(1);
    }

    @Test
    void recordIdempotencyReplay_usesScopeTag() {
        metrics.recordIdempotencyReplay(ApiIdempotencyScope.SALE_REGISTER);
        metrics.recordIdempotencyReplay(ApiIdempotencyScope.STOCK_ADJUST);
        assertThat(
                        registry.counter("lojapp.idempotency.replay", "scope", "SALE_REGISTER")
                                .count())
                .isEqualTo(1);
        assertThat(
                        registry.counter("lojapp.idempotency.replay", "scope", "STOCK_ADJUST")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void recordNfeImport_outcomesAreSeparateSeries() {
        metrics.recordNfeImportSuccess();
        metrics.recordNfeImportDuplicateKey();
        assertThat(registry.get("lojapp.nfe.imports").tags("outcome", "success").counter().count())
                .isEqualTo(1);
        assertThat(
                        registry.get("lojapp.nfe.imports")
                                .tags("outcome", "duplicate_key")
                                .counter()
                                .count())
                .isEqualTo(1);
    }
}
