package com.lojapp.domain.sale;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.exception.domain.LojappDomainException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Linha de venda validada para persistência: quantidade, preço de venda e custo unitário efectivo.
 *
 * <p>Centraliza invariantes que antes viviam só no caso de uso; o {@link SaleRequest} continua a
 * validar entrada (Bean Validation), aqui reforçamos a regra de negócio.
 */
public record SaleRegistrationLine(BigDecimal quantity, BigDecimal unitPrice, BigDecimal unitCost) {

    public SaleRegistrationLine {
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(unitCost, "unitCost");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LojappDomainException(ApiErrorCode.BAD_REQUEST, "quantidade inválida");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new LojappDomainException(ApiErrorCode.BAD_REQUEST, "unitPrice inválido");
        }
        if (unitCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LojappDomainException(ApiErrorCode.BAD_REQUEST, "unitCost inválido");
        }
    }

    /**
     * Resolve custo: usa {@link SaleRequest#unitCost()} se presente; caso contrário o custo actual
     * do produto (não pode ser nulo na persistência típica; zero é rejeitado pelo compact
     * constructor).
     */
    public static SaleRegistrationLine fromRequest(SaleRequest request, BigDecimal productCostPrice) {
        Objects.requireNonNull(request, "request");
        BigDecimal resolvedCost =
                request.unitCost() != null ? request.unitCost() : Objects.requireNonNull(productCostPrice, "productCostPrice");
        return new SaleRegistrationLine(request.quantity(), request.unitPrice(), resolvedCost);
    }
}
