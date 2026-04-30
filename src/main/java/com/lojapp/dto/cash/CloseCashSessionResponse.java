package com.lojapp.dto.cash;

import java.math.BigDecimal;
import java.time.Instant;

public record CloseCashSessionResponse(
        Long cashSessionId,
        BigDecimal expectedAmount,
        BigDecimal countedAmount,
        BigDecimal differenceAmount,
        Instant closedAt,
        String status) {}
