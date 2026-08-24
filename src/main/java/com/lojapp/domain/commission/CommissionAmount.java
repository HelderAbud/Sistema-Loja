package com.lojapp.domain.commission;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CommissionAmount {

    private CommissionAmount() {}

    public static BigDecimal of(BigDecimal base, BigDecimal percent) {
        if (base == null || percent == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return base.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
