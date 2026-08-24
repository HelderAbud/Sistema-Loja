package com.lojapp.dto.sale;

import com.lojapp.exception.domain.PosSaleDuplicateProductException;
import com.lojapp.exception.domain.PosSaleLinesRequiredException;
import com.lojapp.exception.domain.PosSaleTooManyLinesException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record PosSaleFinalizeRequest(
        @NotNull Long cashSessionId,
        Long productId,
        @DecimalMin("0.001") BigDecimal quantity,
        @DecimalMin("0.00") BigDecimal unitPrice,
        BigDecimal unitCost,
        @Size(max = 50) List<@Valid PosSaleLineRequest> items,
        @NotEmpty List<@Valid PosSalePaymentRequest> payments,
        Long sellerId) {

    public static final int MAX_LINES = 50;

    public List<PosSaleLineRequest> resolvedLines() {
        if (items != null && !items.isEmpty()) {
            if (items.size() > MAX_LINES) {
                throw new PosSaleTooManyLinesException(MAX_LINES);
            }
            assertNoDuplicateProducts(items);
            return List.copyOf(items);
        }
        if (productId != null && quantity != null && unitPrice != null) {
            return List.of(new PosSaleLineRequest(productId, quantity, unitPrice, unitCost));
        }
        throw new PosSaleLinesRequiredException();
    }

    private static void assertNoDuplicateProducts(List<PosSaleLineRequest> lines) {
        Set<Long> seen = new HashSet<>();
        for (PosSaleLineRequest line : lines) {
            if (line.productId() != null && !seen.add(line.productId())) {
                throw new PosSaleDuplicateProductException();
            }
        }
    }

    public static PosSaleFinalizeRequest singleItem(
            Long cashSessionId,
            Long productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal unitCost,
            List<PosSalePaymentRequest> payments) {
        return new PosSaleFinalizeRequest(
                cashSessionId, productId, quantity, unitPrice, unitCost, null, payments, null);
    }
}
