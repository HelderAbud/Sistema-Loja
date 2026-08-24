package com.lojapp.dto.commission;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record CommissionRuleRequest(
        Long brandId,
        @NotNull @DecimalMin("0.0000") @DecimalMax("100.0000") BigDecimal percent,
        Instant validFrom) {}
