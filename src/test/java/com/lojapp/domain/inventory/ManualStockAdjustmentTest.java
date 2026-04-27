package com.lojapp.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.exception.domain.LojappDomainException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ManualStockAdjustmentTest {

    @Test
    void fromRequest_trimsReason() {
        ManualStockAdjustment adj =
                ManualStockAdjustment.fromRequest(
                        new StockAdjustmentRequest(1L, new BigDecimal("2"), "  INV  "));
        assertThat(adj.productId()).isEqualTo(1L);
        assertThat(adj.quantity()).isEqualByComparingTo("2");
        assertThat(adj.reason()).isEqualTo("INV");
    }

    @Test
    void fromRequest_whitespaceOnlyReason_rejects() {
        assertThatThrownBy(
                        () ->
                                ManualStockAdjustment.fromRequest(
                                        new StockAdjustmentRequest(1L, BigDecimal.ONE, "   ")))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void constructor_zeroQuantity_rejects() {
        assertThatThrownBy(() -> new ManualStockAdjustment(1L, BigDecimal.ZERO, "x"))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void constructor_reasonTooLong_rejects() {
        String longReason = "x".repeat(501);
        assertThatThrownBy(() -> new ManualStockAdjustment(1L, BigDecimal.ONE, longReason))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }
}
