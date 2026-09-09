package com.lojapp.application.usecases.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.lojapp.dto.cash.CurrentCashSessionResponse;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.SalePaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCurrentCashSessionUseCaseTest {

    @Mock private CashSessionRepository cashSessions;
    @Mock private SalePaymentRepository salePayments;

    private GetCurrentCashSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCurrentCashSessionUseCase(cashSessions, salePayments);
    }

    @Test
    void execute_openSession_cardBucketIncludesCreditAndExpectedIncludesSlip() {
        CashSession session = new CashSession();
        session.setId(3L);
        session.setStatus(CashSessionStatus.OPEN);
        session.setOpeningAmount(new BigDecimal("10.00"));
        session.setOpenedAt(Instant.parse("2026-09-09T12:00:00Z"));

        when(cashSessions.findByUser_IdAndStatus(1L, CashSessionStatus.OPEN))
                .thenReturn(Optional.of(session));
        when(salePayments.sumAmountByCashSessionAndMethod(1L, 3L, PaymentMethod.CASH))
                .thenReturn(new BigDecimal("20.00"));
        when(salePayments.sumAmountByCashSessionAndMethods(
                        eq(1L), eq(3L), eq(PaymentMethod.cardDrawerMethods())))
                .thenReturn(new BigDecimal("35.00"));
        when(salePayments.sumAmountByCashSessionAndMethod(1L, 3L, PaymentMethod.PIX))
                .thenReturn(new BigDecimal("10.00"));
        when(salePayments.sumAmountByCashSession(1L, 3L)).thenReturn(new BigDecimal("80.00"));

        CurrentCashSessionResponse response = useCase.execute(1L);

        assertThat(response.expectedCashAmount()).isEqualByComparingTo("20.00");
        assertThat(response.expectedCardAmount()).isEqualByComparingTo("35.00");
        assertThat(response.expectedPixAmount()).isEqualByComparingTo("10.00");
        assertThat(response.expectedAmount()).isEqualByComparingTo("80.00");
    }
}
