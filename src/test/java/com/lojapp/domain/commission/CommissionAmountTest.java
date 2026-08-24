package com.lojapp.domain.commission;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CommissionAmountTest {

    @Test
    void of_halfUpToCents() {
        assertThat(CommissionAmount.of(new BigDecimal("100.00"), new BigDecimal("12.5")))
                .isEqualByComparingTo("12.50");
        assertThat(CommissionAmount.of(new BigDecimal("10.00"), new BigDecimal("33")))
                .isEqualByComparingTo("3.30");
    }
}
