package com.lojapp.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Linha do kardex (movimento de estoque).")
public record InventoryMovementResponse(
        @Schema(example = "12") Long id,
        @Schema(example = "ADJUSTMENT") String movementType,
        @Schema(description = "Quantidade com sinal: entrada positiva, saída negativa", example = "-3")
                BigDecimal quantity,
        @Schema(example = "MANUAL_ADJUST") String source,
        @Schema(example = "456") Long sourceId,
        @Schema(description = "Motivo do ajuste manual; nulo em venda/NFe", example = "Inventário físico")
                String reason,
        Instant createdAt) {}
