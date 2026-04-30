package com.lojapp.dto.cash;

import java.math.BigDecimal;

public record CloseCashSessionPreviewResponse(
        Long cashSessionId,
        BigDecimal expectedAmount,
        BigDecimal expectedCashAmount,
        BigDecimal expectedCardAmount,
        BigDecimal expectedPixAmount,
        BigDecimal countedAmount,
        BigDecimal differenceAmount,
        BigDecimal toleranceAmount,
        boolean managerApprovalRequired) {}
