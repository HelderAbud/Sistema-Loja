package com.lojapp.dto.sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record PosSaleFinalizeRequest(
        @NotNull Long cashSessionId,
        @NotNull Long productId,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        BigDecimal unitCost,
        @NotEmpty List<@Valid PosSalePaymentRequest> payments) {}
