package com.lojapp.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StockLedgerDeltaTest {

    @Test
    void forSaleDecrease_negatesQuantity() {
        StockLedgerDelta d = StockLedgerDelta.forSaleDecrease(new BigDecimal("3"));
        assertThat(d.signedQuantity()).isEqualByComparingTo("-3");
    }

    @Test
    void forSaleDecrease_nonPositive_rejects() {
        assertThatThrownBy(() -> StockLedgerDelta.forSaleDecrease(BigDecimal.ZERO))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void forNfeEntry_keepsPositive() {
        StockLedgerDelta d = StockLedgerDelta.forNfeEntry(new BigDecimal("10"));
        assertThat(d.signedQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void forManualAdjustment_zero_rejects() {
        assertThatThrownBy(() -> StockLedgerDelta.forManualAdjustment(BigDecimal.ZERO))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void forManualAdjustment_negative_allowed() {
        StockLedgerDelta d = StockLedgerDelta.forManualAdjustment(new BigDecimal("-2"));
        assertThat(d.signedQuantity()).isEqualByComparingTo("-2");
    }
}
