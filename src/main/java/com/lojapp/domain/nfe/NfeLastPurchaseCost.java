package com.lojapp.domain.nfe;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Política de custo no match NFe: o produto passa a ter o último {@code vUnCom} da nota.
 *
 * <p>Não calcula média ponderada. Não altera preço de venda.
 */
public final class NfeLastPurchaseCost {

    private NfeLastPurchaseCost() {}

    public static Optional<BigDecimal> replacementCost(BigDecimal currentCost, BigDecimal nfeUnitCost) {
        if (nfeUnitCost == null) {
            return Optional.empty();
        }
        if (currentCost != null && currentCost.compareTo(nfeUnitCost) == 0) {
            return Optional.empty();
        }
        return Optional.of(nfeUnitCost);
    }
}
