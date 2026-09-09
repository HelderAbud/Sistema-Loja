package com.lojapp.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentMethodTest {

    @Test
    void cardDrawerIncludesLegacyCardAndExplicitCreditDebit() {
        assertThat(PaymentMethod.CARD.countsAsCardInCashDrawer()).isTrue();
        assertThat(PaymentMethod.CREDIT_CARD.countsAsCardInCashDrawer()).isTrue();
        assertThat(PaymentMethod.DEBIT_CARD.countsAsCardInCashDrawer()).isTrue();
        assertThat(PaymentMethod.PIX.countsAsCardInCashDrawer()).isFalse();
        assertThat(PaymentMethod.BANK_SLIP.countsAsCardInCashDrawer()).isFalse();
        assertThat(PaymentMethod.cardDrawerMethods())
                .containsExactlyInAnyOrder(
                        PaymentMethod.CARD, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD);
    }
}
