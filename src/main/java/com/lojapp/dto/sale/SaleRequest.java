package com.lojapp.dto.sale;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * @param unitCost custo unitário na venda; se omitido (null), usa o {@code costPrice} atual do
 *     produto.
 */
public record SaleRequest(
        @NotNull @Positive Long productId,
        @NotNull @DecimalMin("0.001") @Digits(integer = 16, fraction = 3) BigDecimal quantity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal unitPrice,
        @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal unitCost) {}
