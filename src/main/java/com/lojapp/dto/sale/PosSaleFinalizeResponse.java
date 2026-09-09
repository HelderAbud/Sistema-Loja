package com.lojapp.dto.sale;

import java.math.BigDecimal;
import java.time.Instant;

public record PosSaleFinalizeResponse(
        Long saleId,
        Long cashSessionId,
        BigDecimal totalAmount,
        Instant soldAt,
        Long sellerId,
        BigDecimal changeAmount) {

    public PosSaleFinalizeResponse(
            Long saleId, Long cashSessionId, BigDecimal totalAmount, Instant soldAt, Long sellerId) {
        this(saleId, cashSessionId, totalAmount, soldAt, sellerId, BigDecimal.ZERO);
    }
}
