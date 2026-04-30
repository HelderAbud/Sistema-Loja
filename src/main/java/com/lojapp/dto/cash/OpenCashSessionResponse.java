package com.lojapp.dto.cash;

import java.math.BigDecimal;
import java.time.Instant;

public record OpenCashSessionResponse(
        Long cashSessionId, BigDecimal openingAmount, Instant openedAt, String status) {}
