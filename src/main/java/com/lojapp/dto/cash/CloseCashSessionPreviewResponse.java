package com.lojapp.dto.cash;

import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(
                description =
                        "True se |diferença| > tolerância: o close exige managerApproval=true "
                                + "(confirmação do próprio ator, não de um terceiro).")
                boolean managerApprovalRequired) {}
