package com.lojapp.dto.sale;

import com.lojapp.entity.PaymentMethod;
import com.lojapp.entity.PaymentSettlementStatus;
import com.lojapp.entity.SalePayment;
import java.math.BigDecimal;

public record PosSalePaymentView(
        Long paymentId,
        Long saleId,
        Long cashSessionId,
        Long settlementCashSessionId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        PaymentSettlementStatus settlementStatus) {

    public static PosSalePaymentView from(SalePayment payment) {
        Long cashSessionId =
                payment.getSale().getCashSession() == null
                        ? null
                        : payment.getSale().getCashSession().getId();
        Long settlementCashSessionId =
                payment.getSettlementCashSession() == null
                        ? null
                        : payment.getSettlementCashSession().getId();
        return new PosSalePaymentView(
                payment.getId(),
                payment.getSale().getId(),
                cashSessionId,
                settlementCashSessionId,
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getSettlementStatus());
    }
}
