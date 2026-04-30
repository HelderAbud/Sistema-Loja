package com.lojapp.dto.cash;

import java.math.BigDecimal;
import java.time.Instant;

public record CurrentCashSessionResponse(
        boolean open,
        Long cashSessionId,
        BigDecimal openingAmount,
        Instant openedAt,
        BigDecimal expectedAmount,
        BigDecimal expectedCashAmount,
        BigDecimal expectedCardAmount,
        BigDecimal expectedPixAmount) {}
