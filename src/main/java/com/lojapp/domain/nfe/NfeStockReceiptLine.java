package com.lojapp.domain.nfe;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Quantidade de uma linha de NFe aplicável a entrada de stock (compra / devolução tratada como
 * entrada no modelo actual).
 *
 * <p>Garante que o valor é adequado antes de persistir {@link com.lojapp.entity.NfeItem} e de
 * chamar {@link com.lojapp.service.contract.InventoryServiceContract#increaseFromNfe}.
 */
public record NfeStockReceiptLine(BigDecimal quantity) {

    public NfeStockReceiptLine {
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST,
                    "Quantidade da linha NFe inválida para entrada em stock");
        }
    }

    public static NfeStockReceiptLine of(BigDecimal quantityFromXmlLine) {
        return new NfeStockReceiptLine(quantityFromXmlLine);
    }
}
