package com.lojapp.dto.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OpenCashSessionRequest(@NotNull @DecimalMin("0.00") BigDecimal openingAmount) {}
