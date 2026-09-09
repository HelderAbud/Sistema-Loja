package com.lojapp.dto.sale;

import com.lojapp.entity.PaymentMethod;
import com.lojapp.entity.PaymentSettlementStatus;
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
        @DecimalMin("0.01") BigDecimal receivedAmount,
        PaymentSettlementStatus settlementStatus) {

    public PosSalePaymentRequest {
        cardBrand = blankToNull(cardBrand);
        transactionId = blankToNull(transactionId);
        endToEndId = blankToNull(endToEndId);
        if (settlementStatus == null) {
            settlementStatus = PaymentSettlementStatus.CONFIRMED;
        }
    }

    public PosSalePaymentRequest(PaymentMethod paymentMethod, BigDecimal amount) {
        this(paymentMethod, amount, null, null, null, null, null, null);
    }

    public PosSalePaymentRequest(
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String cardBrand,
            Integer installments,
            String transactionId,
            String endToEndId,
            BigDecimal receivedAmount) {
        this(
                paymentMethod,
                amount,
                cardBrand,
                installments,
                transactionId,
                endToEndId,
                receivedAmount,
                null);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
