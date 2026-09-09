package com.lojapp.application.usecases.sale;

import com.lojapp.application.contract.ConfirmPosSalePaymentUseCaseContract;
import com.lojapp.dto.sale.PosSalePaymentView;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.PaymentSettlementStatus;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SalePayment;
import com.lojapp.exception.domain.PosSalePaymentNotFoundException;
import com.lojapp.exception.domain.SaleAlreadyCancelledException;
import com.lojapp.exception.domain.SalePaymentConfirmSessionClosedException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmPosSalePaymentUseCase implements ConfirmPosSalePaymentUseCaseContract {

    private final SalePaymentRepository salePayments;
    private final CashSessionRepository cashSessions;
    private final AuditService auditService;

    public ConfirmPosSalePaymentUseCase(
            SalePaymentRepository salePayments,
            CashSessionRepository cashSessions,
            AuditService auditService) {
        this.salePayments = salePayments;
        this.cashSessions = cashSessions;
        this.auditService = auditService;
    }

    @Transactional
    public PosSalePaymentView execute(long userId, long saleId, long paymentId) {
        SalePayment payment =
                salePayments
                        .findByIdAndUserIdWithSale(paymentId, userId)
                        .orElseThrow(PosSalePaymentNotFoundException::new);
        Sale sale = payment.getSale();
        if (!Long.valueOf(saleId).equals(sale.getId())) {
            throw new PosSalePaymentNotFoundException();
        }
        if (sale.getCancelledAt() != null) {
            throw new SaleAlreadyCancelledException();
        }
        if (payment.getSettlementStatus() == PaymentSettlementStatus.CONFIRMED) {
            return PosSalePaymentView.from(payment);
        }
        CashSession settlementSession = resolveSettlementSession(userId, sale);
        payment.setSettlementStatus(PaymentSettlementStatus.CONFIRMED);
        payment.setSettlementCashSession(settlementSession);
        salePayments.save(payment);
        auditService.log(
                userId,
                "POS_PAYMENT_CONFIRMED",
                "saleId=%d paymentId=%d amount=%s method=%s cashSessionId=%d"
                        .formatted(
                                sale.getId(),
                                payment.getId(),
                                payment.getAmount(),
                                payment.getPaymentMethod(),
                                settlementSession.getId()));
        return PosSalePaymentView.from(payment);
    }

    private CashSession resolveSettlementSession(long userId, Sale sale) {
        CashSession origin = sale.getCashSession();
        if (origin != null && origin.getStatus() == CashSessionStatus.OPEN) {
            return origin;
        }
        return cashSessions
                .findByUser_IdAndStatus(userId, CashSessionStatus.OPEN)
                .orElseThrow(SalePaymentConfirmSessionClosedException::new);
    }
}
