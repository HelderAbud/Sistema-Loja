package com.lojapp.dto.sale;

import com.lojapp.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PosSalePaymentRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Size(max = 40) String cardBrand,
        @Min(1) Integer installments,
        @Size(max = 80) String transactionId,
        @Size(max = 32) String endToEndId,
        @DecimalMin("0.01") BigDecimal receivedAmount) {

    public PosSalePaymentRequest {
        cardBrand = blankToNull(cardBrand);
        transactionId = blankToNull(transactionId);
        endToEndId = blankToNull(endToEndId);
    }

    public PosSalePaymentRequest(PaymentMethod paymentMethod, BigDecimal amount) {
        this(paymentMethod, amount, null, null, null, null, null);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
