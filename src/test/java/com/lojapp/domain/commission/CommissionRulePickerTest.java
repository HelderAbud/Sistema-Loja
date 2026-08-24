package com.lojapp.domain.commission;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommissionRulePickerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-12-01T00:00:00Z");

    @Test
    void pick_futureRule_isIgnored() {
        var future =
                new CommissionRuleSnapshot(1L, null, new BigDecimal("10"), T2);
        assertThat(CommissionRulePicker.pick(List.of(future), null, T1)).isEmpty();
    }

    @Test
    void pick_brandBeatsOlderGlobal() {
        var global = new CommissionRuleSnapshot(1L, null, new BigDecimal("5"), T0);
        var brand = new CommissionRuleSnapshot(2L, 8L, new BigDecimal("12"), T0);
        assertThat(CommissionRulePicker.pick(List.of(global, brand), 8L, T1))
                .contains(brand);
    }

    @Test
    void pick_noBrand_usesLatestGlobal() {
        var oldG = new CommissionRuleSnapshot(1L, null, new BigDecimal("5"), T0);
        var newG = new CommissionRuleSnapshot(3L, null, new BigDecimal("7"), T1);
        assertThat(CommissionRulePicker.pick(List.of(oldG, newG), 8L, T1)).contains(newG);
    }

    @Test
    void pick_unknownBrand_fallsBackToGlobal() {
        var global = new CommissionRuleSnapshot(1L, null, new BigDecimal("5"), T0);
        var otherBrand = new CommissionRuleSnapshot(2L, 9L, new BigDecimal("20"), T0);
        assertThat(CommissionRulePicker.pick(List.of(global, otherBrand), 8L, T1))
                .contains(global);
    }
}
