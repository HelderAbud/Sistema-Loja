package com.lojapp.dto.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CloseCashSessionRequest(
        @NotNull Long cashSessionId,
        @NotNull @DecimalMin("0.00") BigDecimal countedAmount,
        String differenceReason,
        boolean managerApproval) {}
