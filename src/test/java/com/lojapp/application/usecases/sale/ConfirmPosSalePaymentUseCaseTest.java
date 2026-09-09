package com.lojapp.application.usecases.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.dto.sale.PosSalePaymentView;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.entity.PaymentSettlementStatus;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SalePayment;
import com.lojapp.exception.domain.PosSalePaymentNotFoundException;
import com.lojapp.exception.domain.SaleAlreadyCancelledException;
import com.lojapp.exception.domain.SalePaymentConfirmSessionClosedException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.service.AuditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmPosSalePaymentUseCaseTest {

    @Mock private SalePaymentRepository salePayments;
    @Mock private CashSessionRepository cashSessions;
    @Mock private AuditService auditService;

    private ConfirmPosSalePaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmPosSalePaymentUseCase(salePayments, cashSessions, auditService);
    }

    @Test
    void execute_whenPendingOnOpenSession_confirmsIntoOriginSession() {
        SalePayment payment = pendingPixOnOpenSale();
        when(salePayments.findByIdAndUserIdWithSale(5L, 1L)).thenReturn(Optional.of(payment));

        PosSalePaymentView view = useCase.execute(1L, 91L, 5L);

        assertThat(payment.getSettlementStatus()).isEqualTo(PaymentSettlementStatus.CONFIRMED);
        assertThat(payment.getSettlementCashSession().getId()).isEqualTo(7L);
        assertThat(view.settlementCashSessionId()).isEqualTo(7L);
        verify(salePayments).save(payment);
        verify(auditService)
                .log(
                        1L,
                        "POS_PAYMENT_CONFIRMED",
                        "saleId=91 paymentId=5 amount=20.00 method=PIX cashSessionId=7");
    }

    @Test
    void execute_whenOriginClosedAndCurrentOpen_confirmsIntoCurrentSession() {
        SalePayment payment = pendingPixOnOpenSale();
        payment.getSale().getCashSession().setStatus(CashSessionStatus.CLOSED);
        CashSession current = new CashSession();
        current.setId(8L);
        current.setStatus(CashSessionStatus.OPEN);
        when(salePayments.findByIdAndUserIdWithSale(5L, 1L)).thenReturn(Optional.of(payment));
        when(cashSessions.findByUser_IdAndStatus(1L, CashSessionStatus.OPEN))
                .thenReturn(Optional.of(current));

        PosSalePaymentView view = useCase.execute(1L, 91L, 5L);

        assertThat(payment.getSettlementCashSession()).isSameAs(current);
        assertThat(view.settlementCashSessionId()).isEqualTo(8L);
        verify(salePayments).save(payment);
    }

    @Test
    void execute_whenAlreadyConfirmed_isIdempotent() {
        SalePayment payment = pendingPixOnOpenSale();
        payment.setSettlementStatus(PaymentSettlementStatus.CONFIRMED);
        when(salePayments.findByIdAndUserIdWithSale(5L, 1L)).thenReturn(Optional.of(payment));

        PosSalePaymentView view = useCase.execute(1L, 91L, 5L);

        assertThat(view.settlementStatus()).isEqualTo(PaymentSettlementStatus.CONFIRMED);
        verify(salePayments, never()).save(payment);
        verify(auditService, never())
                .log(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_whenSaleCancelled_throws() {
        SalePayment payment = pendingPixOnOpenSale();
        payment.getSale().setCancelledAt(Instant.parse("2026-09-09T13:00:00Z"));
        when(salePayments.findByIdAndUserIdWithSale(5L, 1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase.execute(1L, 91L, 5L))
                .isInstanceOf(SaleAlreadyCancelledException.class);
        verify(salePayments, never()).save(payment);
    }

    @Test
    void execute_whenOriginClosedAndNoOpenSession_throws() {
        SalePayment payment = pendingPixOnOpenSale();
        payment.getSale().getCashSession().setStatus(CashSessionStatus.CLOSED);
        when(salePayments.findByIdAndUserIdWithSale(5L, 1L)).thenReturn(Optional.of(payment));
        when(cashSessions.findByUser_IdAndStatus(1L, CashSessionStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(1L, 91L, 5L))
                .isInstanceOf(SalePaymentConfirmSessionClosedException.class);
        verify(salePayments, never()).save(payment);
    }

    @Test
    void execute_whenSaleIdMismatch_throwsNotFound() {
        SalePayment payment = pendingPixOnOpenSale();
        when(salePayments.findByIdAndUserIdWithSale(5L, 1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase.execute(1L, 8L, 5L))
                .isInstanceOf(PosSalePaymentNotFoundException.class);
    }

    @Test
    void execute_whenMissing_throwsNotFound() {
        when(salePayments.findByIdAndUserIdWithSale(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(1L, 91L, 5L))
                .isInstanceOf(PosSalePaymentNotFoundException.class);
    }

    private static SalePayment pendingPixOnOpenSale() {
        CashSession session = new CashSession();
        session.setId(7L);
        session.setStatus(CashSessionStatus.OPEN);
        Sale sale = new Sale();
        sale.setId(91L);
        sale.setCashSession(session);
        SalePayment payment = new SalePayment();
        payment.setId(5L);
        payment.setSale(sale);
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setAmount(new BigDecimal("20.00"));
        payment.setSettlementStatus(PaymentSettlementStatus.PENDING);
        return payment;
    }
}
