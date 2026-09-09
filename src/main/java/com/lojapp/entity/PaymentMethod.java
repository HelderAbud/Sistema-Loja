package com.lojapp.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Meio de pagamento persistido em {@code sale_payments}. {@link #CARD} permanece por
 * compatibilidade com o PDV e dados já gravados; crédito/débito novos usam valores explícitos.
 */
public enum PaymentMethod {
    CASH,
    CARD,
    PIX,
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    BANK_SLIP,
    OTHER;

    private static final Set<PaymentMethod> CARD_DRAWER =
            EnumSet.of(CARD, CREDIT_CARD, DEBIT_CARD);

    public boolean countsAsCardInCashDrawer() {
        return CARD_DRAWER.contains(this);
    }

    public boolean allowsCardBrand() {
        return countsAsCardInCashDrawer();
    }

    public boolean allowsInstallments() {
        return this == CARD || this == CREDIT_CARD;
    }

    public boolean allowsReceivedAmount() {
        return this == CASH;
    }

    public boolean allowsEndToEndId() {
        return this == PIX;
    }

    public static Set<PaymentMethod> cardDrawerMethods() {
        return EnumSet.copyOf(CARD_DRAWER);
    }
}
