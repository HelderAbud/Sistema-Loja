package com.lojapp.dto.sale;

import com.lojapp.entity.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

public record SalesPaymentsSummaryResponse(
        Slice sold, Slice settled, Slice openPending) {

    public record Slice(
            BigDecimal confirmedTotal,
            BigDecimal pendingTotal,
            List<PaymentMethodBreakdown> methods) {}

    public record PaymentMethodBreakdown(
            PaymentMethod paymentMethod, BigDecimal confirmedAmount, BigDecimal pendingAmount) {}
}
