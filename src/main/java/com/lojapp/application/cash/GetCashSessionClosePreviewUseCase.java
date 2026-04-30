package com.lojapp.application.cash;

import com.lojapp.application.contract.GetCashSessionClosePreviewUseCaseContract;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCashSessionClosePreviewUseCase implements GetCashSessionClosePreviewUseCaseContract {

    private final CashSessionRepository cashSessions;
    private final SalePaymentRepository salePayments;
    private final PosProperties posProperties;

    public GetCashSessionClosePreviewUseCase(
            CashSessionRepository cashSessions,
            SalePaymentRepository salePayments,
            PosProperties posProperties) {
        this.cashSessions = cashSessions;
        this.salePayments = salePayments;
        this.posProperties = posProperties;
    }

    @Transactional(readOnly = true)
    public CloseCashSessionPreviewResponse execute(
            long userId, long cashSessionId, BigDecimal countedAmount) {
        CashSession cashSession =
                cashSessions
                        .findByIdAndUser_Id(cashSessionId, userId)
                        .orElseThrow(CashSessionNotFoundException::new);
        if (cashSession.getStatus() != CashSessionStatus.OPEN) {
            throw new CashSessionNotOpenException();
        }

        BigDecimal cash =
                salePayments.sumAmountByCashSessionAndMethod(userId, cashSessionId, PaymentMethod.CASH);
        BigDecimal card =
                salePayments.sumAmountByCashSessionAndMethod(userId, cashSessionId, PaymentMethod.CARD);
        BigDecimal pix =
                salePayments.sumAmountByCashSessionAndMethod(userId, cashSessionId, PaymentMethod.PIX);
        BigDecimal expected = cash.add(card).add(pix);

        BigDecimal normalizedCounted = countedAmount;
        BigDecimal difference = null;
        boolean managerApprovalRequired = false;
        if (normalizedCounted != null) {
            difference = normalizedCounted.subtract(expected);
            managerApprovalRequired =
                    difference.abs().compareTo(posProperties.closeToleranceAmount()) > 0;
        }

        return new CloseCashSessionPreviewResponse(
                cashSessionId,
                expected,
                cash,
                card,
                pix,
                normalizedCounted,
                difference,
                posProperties.closeToleranceAmount(),
                managerApprovalRequired);
    }
}
