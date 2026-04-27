package com.lojapp.domain.nfe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NfeStockReceiptLineTest {

    @Test
    void of_positive_ok() {
        NfeStockReceiptLine line = NfeStockReceiptLine.of(new BigDecimal("12.5"));
        assertThat(line.quantity()).isEqualByComparingTo("12.5");
    }

    @Test
    void of_zero_rejects() {
        assertThatThrownBy(() -> NfeStockReceiptLine.of(BigDecimal.ZERO))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void of_null_rejects() {
        assertThatThrownBy(() -> NfeStockReceiptLine.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
