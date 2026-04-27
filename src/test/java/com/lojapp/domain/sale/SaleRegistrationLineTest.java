package com.lojapp.domain.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.exception.domain.LojappDomainException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SaleRegistrationLineTest {

    @Test
    void fromRequest_usesExplicitUnitCost() {
        SaleRequest req =
                new SaleRequest(1L, new BigDecimal("2"), new BigDecimal("10.00"), new BigDecimal("4.50"));
        SaleRegistrationLine line = SaleRegistrationLine.fromRequest(req, new BigDecimal("99.00"));
        assertThat(line.quantity()).isEqualByComparingTo("2");
        assertThat(line.unitPrice()).isEqualByComparingTo("10.00");
        assertThat(line.unitCost()).isEqualByComparingTo("4.50");
    }

    @Test
    void fromRequest_whenUnitCostNull_usesProductCost() {
        SaleRequest req = new SaleRequest(1L, new BigDecimal("1"), new BigDecimal("8.00"), null);
        SaleRegistrationLine line = SaleRegistrationLine.fromRequest(req, new BigDecimal("3.25"));
        assertThat(line.unitCost()).isEqualByComparingTo("3.25");
    }

    @Test
    void fromRequest_whenResolvedCostNotPositive_rejects() {
        SaleRequest req = new SaleRequest(1L, new BigDecimal("1"), new BigDecimal("8.00"), null);
        assertThatThrownBy(() -> SaleRegistrationLine.fromRequest(req, BigDecimal.ZERO))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void constructor_negativeUnitPrice_rejects() {
        assertThatThrownBy(
                        () ->
                                new SaleRegistrationLine(
                                        new BigDecimal("1"), new BigDecimal("-0.01"), new BigDecimal("1")))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }
}
