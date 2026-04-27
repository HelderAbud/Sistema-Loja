package com.lojapp.domain.inventory;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Delta assinado aplicado ao saldo em {@code inventory_balances} (entradas positivas, saídas
 * negativas), alinhado a {@link com.lojapp.entity.InventoryMovementType}.
 *
 * <p>Centraliza invariantes de quantidade que antes estavam espalhadas em {@code
 * InventoryService}.
 */
public record StockLedgerDelta(BigDecimal signedQuantity) {

    public StockLedgerDelta {
        Objects.requireNonNull(signedQuantity, "signedQuantity");
    }

    /** Venda: quantidade física vendida ({@code > 0}); o ledger regista delta negativo. */
    public static StockLedgerDelta forSaleDecrease(BigDecimal quantitySold) {
        assertPositivePhysical(
                quantitySold, "Quantidade da venda deve ser positiva");
        return new StockLedgerDelta(quantitySold.negate());
    }

    /** Entrada por NFe: quantidade recebida ({@code > 0}). */
    public static StockLedgerDelta forNfeEntry(BigDecimal quantityReceived) {
        assertPositivePhysical(
                quantityReceived, "Quantidade da NFe deve ser positiva");
        return new StockLedgerDelta(quantityReceived);
    }

    /** Cancelamento de venda: quantidade a repor no armazém ({@code > 0}); delta positivo. */
    public static StockLedgerDelta forSaleCancellationRestore(BigDecimal quantitySold) {
        assertPositivePhysical(
                quantitySold, "Quantidade reposta deve ser positiva");
        return new StockLedgerDelta(quantitySold);
    }

    /**
     * Ajuste manual: delta com sinal (entrada positiva, saída negativa), não nulo e não zero.
     */
    public static StockLedgerDelta forManualAdjustment(BigDecimal signedDelta) {
        Objects.requireNonNull(signedDelta, "signedDelta");
        if (signedDelta.compareTo(BigDecimal.ZERO) == 0) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST, "quantity não pode ser zero");
        }
        return new StockLedgerDelta(signedDelta);
    }

    private static void assertPositivePhysical(BigDecimal quantity, String message) {
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LojappDomainException(ApiErrorCode.BAD_REQUEST, message);
        }
    }
}
