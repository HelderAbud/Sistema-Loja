package com.lojapp.domain.commission;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionRuleSnapshot(
        Long id, Long brandId, BigDecimal percent, Instant validFrom) {}
