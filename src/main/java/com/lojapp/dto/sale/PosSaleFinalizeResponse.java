package com.lojapp.dto.sale;

import java.math.BigDecimal;
import java.time.Instant;

public record PosSaleFinalizeResponse(
        Long saleId, Long cashSessionId, BigDecimal totalAmount, Instant soldAt) {}
