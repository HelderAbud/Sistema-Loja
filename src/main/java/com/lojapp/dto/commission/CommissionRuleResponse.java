package com.lojapp.dto.commission;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionRuleResponse(
        Long id, Long brandId, BigDecimal percent, Instant validFrom, Instant createdAt) {}
