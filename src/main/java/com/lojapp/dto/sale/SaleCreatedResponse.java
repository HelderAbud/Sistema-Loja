package com.lojapp.dto.sale;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Resposta de criação de venda.")
public record SaleCreatedResponse(
        long id,
        long productId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal unitCost,
        Instant soldAt) {}
