package com.lojapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Garante que o stack Micrometer + Brave arranca com tracing ligado e export Zipkin desligado
 * (cenário típico: correlacionar logs sem colector).
 */
@SpringBootTest
@TestPropertySource(
        properties = {
            "management.tracing.enabled=true",
            "management.tracing.sampling.probability=1.0",
            "management.zipkin.tracing.export.enabled=false"
        })
class MicrometerTracingBridgeContextTest {

    @Autowired private Tracer tracer;

    @Test
    void contextLoads_withTracingWithoutZipkinExport() {
        assertThat(tracer).isNotNull();
    }
}
