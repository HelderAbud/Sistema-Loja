package com.lojapp.dto.cash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CloseCashSessionRequest(
        @NotNull Long cashSessionId,
        @NotNull @DecimalMin("0.00") BigDecimal countedAmount,
        String differenceReason,
        @Schema(
                description =
                        "Auto-declaração do próprio ator: confirma que reviu a diferença acima da tolerância. "
                                + "Não é aprovação de um gerente na mesma loja (MVP: uma conta = uma loja).")
                boolean managerApproval) {}
