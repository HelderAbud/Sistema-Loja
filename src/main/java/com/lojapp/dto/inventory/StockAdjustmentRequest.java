package com.lojapp.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record StockAdjustmentRequest(
        @NotNull @Positive @Schema(description = "Id do produto da loja", example = "12") Long productId,
        @NotNull @Schema(description = "Delta de stock (positivo entrada, negativo saída)", example = "-2")
                @Digits(integer = 16, fraction = 3)
                BigDecimal quantity,
        @NotBlank
                @Size(max = 500)
                @Schema(
                        description =
                                "Motivo obrigatório do ajuste (texto livre; espaços à volta são ignorados no servidor)",
                        example = "INVENTARIO_FISICO",
                        maxLength = 500)
                String reason) {

    @AssertTrue(message = "quantity não pode ser zero")
    public boolean isQuantityNonZero() {
        return quantity != null && quantity.compareTo(BigDecimal.ZERO) != 0;
    }
}
