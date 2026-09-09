package com.lojapp.application.usecases.sale;

import com.lojapp.dto.sale.PosSalePaymentRequest;
import com.lojapp.exception.domain.PosSalePaymentDetailsInvalidException;
import java.math.BigDecimal;
import java.util.List;

public final class PosSalePaymentDetails {

    private PosSalePaymentDetails() {}

    public static void validate(PosSalePaymentRequest payment) {
        if (payment.receivedAmount() != null) {
            if (!payment.paymentMethod().allowsReceivedAmount()) {
                throw new PosSalePaymentDetailsInvalidException(
                        "receivedAmount só é permitido para pagamento em dinheiro (CASH).");
            }
            if (payment.receivedAmount().compareTo(payment.amount()) < 0) {
                throw new PosSalePaymentDetailsInvalidException(
                        "receivedAmount não pode ser menor que o amount aplicado à venda.");
            }
        }
        if (hasText(payment.cardBrand()) && !payment.paymentMethod().allowsCardBrand()) {
            throw new PosSalePaymentDetailsInvalidException(
                    "cardBrand só é permitido para CARD, CREDIT_CARD ou DEBIT_CARD.");
        }
        if (payment.installments() != null && !payment.paymentMethod().allowsInstallments()) {
            throw new PosSalePaymentDetailsInvalidException(
                    "installments só é permitido para CARD ou CREDIT_CARD.");
        }
        if (hasText(payment.endToEndId()) && !payment.paymentMethod().allowsEndToEndId()) {
            throw new PosSalePaymentDetailsInvalidException(
                    "endToEndId só é permitido para PIX.");
        }
    }

    public static BigDecimal totalChange(List<PosSalePaymentRequest> payments) {
        BigDecimal change = BigDecimal.ZERO;
        for (PosSalePaymentRequest payment : payments) {
            if (payment.receivedAmount() != null) {
                change = change.add(payment.receivedAmount().subtract(payment.amount()));
            }
        }
        return change;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
