package com.lojapp.domain.sale;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.SaleAlreadyCancelledException;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Venda que ainda pode ser cancelada: não tinha {@code cancelledAt} e tem quantidade vendida
 * positiva (coerente com reposição de stock).
 */
public record SalePendingCancellation(long saleId, BigDecimal quantityToRestore) {

    public SalePendingCancellation {
        if (quantityToRestore == null || quantityToRestore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST,
                    "Quantidade da venda inválida para cancelamento");
        }
    }

    /**
     * Interpreta o estado persistido da venda antes de marcar cancelamento.
     *
     * @param cancelledAt instante já gravado, se existir
     * @param lineQuantity quantidade vendida na linha (positiva no modelo actual)
     */
    public static SalePendingCancellation fromPersistedState(
            long saleId, Instant cancelledAt, BigDecimal lineQuantity) {
        if (cancelledAt != null) {
            throw new SaleAlreadyCancelledException();
        }
        return new SalePendingCancellation(saleId, lineQuantity);
    }
}
