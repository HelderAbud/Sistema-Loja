package com.lojapp.dto.commission;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionAccrualResponse(
        Long id,
        Long saleId,
        Long sellerId,
        String sellerName,
        Long brandId,
        String brandName,
        BigDecimal baseAmount,
        BigDecimal percent,
        BigDecimal amount,
        Instant createdAt) {}
