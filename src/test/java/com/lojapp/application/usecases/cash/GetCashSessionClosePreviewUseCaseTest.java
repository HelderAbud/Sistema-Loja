package com.lojapp.application.usecases.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.config.PosProperties;
import com.lojapp.dto.cash.CloseCashSessionPreviewResponse;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.exception.domain.CashSessionNotFoundException;
import com.lojapp.exception.domain.CashSessionNotOpenException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.SalePaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCashSessionClosePreviewUseCaseTest {

    private static final long STORE_USER_ID = 1L;
    private static final long SESSION_ID = 40L;
    private static final BigDecimal TOLERANCE = new BigDecimal("10.00");

    @Mock private CashSessionRepository cashSessions;
    @Mock private SalePaymentRepository salePayments;

    private GetCashSessionClosePreviewUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase =
                new GetCashSessionClosePreviewUseCase(
                        cashSessions, salePayments, new PosProperties(TOLERANCE));
    }

    @Test
    void execute_whenSessionMissing_throwsWithoutSummingPayments() {
        when(cashSessions.findByIdAndUser_Id(SESSION_ID, STORE_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(STORE_USER_ID, SESSION_ID, new BigDecimal("100.00")))
                .isInstanceOf(CashSessionNotFoundException.class);

        verifyNoPaymentSums();
    }

    @Test
    void execute_whenSessionClosed_throwsWithoutSummingPayments() {
        CashSession closed = openSession();
        closed.setStatus(CashSessionStatus.CLOSED);
        when(cashSessions.findByIdAndUser_Id(SESSION_ID, STORE_USER_ID)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> useCase.execute(STORE_USER_ID, SESSION_ID, null))
                .isInstanceOf(CashSessionNotOpenException.class);

        verifyNoPaymentSums();
    }

    @Test
    void execute_withoutCountedAmount_returnsDrawerBreakdownAndNoDifference() {
        stubOpenSession();
        stubPayments("100.00", "40.00", "35.00", "25.00");

        CloseCashSessionPreviewResponse preview = useCase.execute(STORE_USER_ID, SESSION_ID, null);

        assertThat(preview.cashSessionId()).isEqualTo(SESSION_ID);
        assertThat(preview.expectedAmount()).isEqualByComparingTo("100.00");
        assertThat(preview.expectedCashAmount()).isEqualByComparingTo("40.00");
        assertThat(preview.expectedCardAmount()).isEqualByComparingTo("35.00");
        assertThat(preview.expectedPixAmount()).isEqualByComparingTo("25.00");
        assertThat(preview.countedAmount()).isNull();
        assertThat(preview.differenceAmount()).isNull();
        assertThat(preview.toleranceAmount()).isEqualByComparingTo("10.00");
        assertThat(preview.managerApprovalRequired()).isFalse();
    }

    @Test
    void execute_whenDifferenceEqualsTolerance_doesNotRequireApproval() {
        stubOpenSession();
        stubPayments("100.00", "100.00", "0.00", "0.00");

        CloseCashSessionPreviewResponse preview =
                useCase.execute(STORE_USER_ID, SESSION_ID, new BigDecimal("110.00"));

        assertThat(preview.differenceAmount()).isEqualByComparingTo("10.00");
        assertThat(preview.managerApprovalRequired()).isFalse();
    }

    @Test
    void execute_whenDifferenceExceedsTolerance_requiresApproval() {
        stubOpenSession();
        stubPayments("100.00", "80.00", "20.00", "0.00");

        CloseCashSessionPreviewResponse preview =
                useCase.execute(STORE_USER_ID, SESSION_ID, new BigDecimal("80.00"));

        assertThat(preview.differenceAmount()).isEqualByComparingTo("-20.00");
        assertThat(preview.countedAmount()).isEqualByComparingTo("80.00");
        assertThat(preview.managerApprovalRequired()).isTrue();
    }

    private void stubOpenSession() {
        when(cashSessions.findByIdAndUser_Id(SESSION_ID, STORE_USER_ID))
                .thenReturn(Optional.of(openSession()));
    }

    private void stubPayments(String expected, String cash, String card, String pix) {
        when(salePayments.sumAmountByCashSession(STORE_USER_ID, SESSION_ID))
                .thenReturn(new BigDecimal(expected));
        when(salePayments.sumAmountByCashSessionAndMethod(STORE_USER_ID, SESSION_ID, PaymentMethod.CASH))
                .thenReturn(new BigDecimal(cash));
        when(salePayments.sumAmountByCashSessionAndMethod(STORE_USER_ID, SESSION_ID, PaymentMethod.PIX))
                .thenReturn(new BigDecimal(pix));
        when(salePayments.sumAmountByCashSessionAndMethods(
                        STORE_USER_ID, SESSION_ID, PaymentMethod.cardDrawerMethods()))
                .thenReturn(new BigDecimal(card));
    }

    private CashSession openSession() {
        CashSession session = new CashSession();
        session.setId(SESSION_ID);
        session.setStatus(CashSessionStatus.OPEN);
        return session;
    }

    private void verifyNoPaymentSums() {
        verify(salePayments, never()).sumAmountByCashSession(anyLong(), anyLong());
        verify(salePayments, never()).sumAmountByCashSessionAndMethod(anyLong(), anyLong(), any());
        verify(salePayments, never()).sumAmountByCashSessionAndMethods(anyLong(), anyLong(), any());
    }
}
