package com.lojapp.domain.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.SaleAlreadyCancelledException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SalePendingCancellationTest {

    @Test
    void fromPersistedState_whenAlreadyCancelled_throws() {
        assertThatThrownBy(
                        () ->
                                SalePendingCancellation.fromPersistedState(
                                        1L, Instant.parse("2026-01-01T00:00:00Z"), BigDecimal.ONE))
                .isInstanceOf(SaleAlreadyCancelledException.class);
    }

    @Test
    void fromPersistedState_whenOpen_returnsPending() {
        SalePendingCancellation p =
                SalePendingCancellation.fromPersistedState(99L, null, new BigDecimal("4"));
        assertThat(p.saleId()).isEqualTo(99L);
        assertThat(p.quantityToRestore()).isEqualByComparingTo("4");
    }

    @Test
    void fromPersistedState_whenQuantityNotPositive_throws() {
        assertThatThrownBy(() -> SalePendingCancellation.fromPersistedState(1L, null, BigDecimal.ZERO))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }
}
