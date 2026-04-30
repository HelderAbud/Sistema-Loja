package com.lojapp.application.cash;

import com.lojapp.application.contract.GetCurrentCashSessionUseCaseContract;
import com.lojapp.dto.cash.CurrentCashSessionResponse;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.SalePaymentRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentCashSessionUseCase implements GetCurrentCashSessionUseCaseContract {

    private final CashSessionRepository cashSessions;
    private final SalePaymentRepository salePayments;

    public GetCurrentCashSessionUseCase(
            CashSessionRepository cashSessions, SalePaymentRepository salePayments) {
        this.cashSessions = cashSessions;
        this.salePayments = salePayments;
    }

    @Transactional(readOnly = true)
    public CurrentCashSessionResponse execute(long userId) {
        CashSession cashSession =
                cashSessions.findByUser_IdAndStatus(userId, CashSessionStatus.OPEN).orElse(null);
        if (cashSession == null) {
            return new CurrentCashSessionResponse(
                    false, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal cash =
                salePayments.sumAmountByCashSessionAndMethod(
                        userId, cashSession.getId(), PaymentMethod.CASH);
        BigDecimal card =
                salePayments.sumAmountByCashSessionAndMethod(
                        userId, cashSession.getId(), PaymentMethod.CARD);
        BigDecimal pix =
                salePayments.sumAmountByCashSessionAndMethod(
                        userId, cashSession.getId(), PaymentMethod.PIX);
        BigDecimal expected = cash.add(card).add(pix);

        return new CurrentCashSessionResponse(
                true,
                cashSession.getId(),
                cashSession.getOpeningAmount(),
                cashSession.getOpenedAt(),
                expected,
                cash,
                card,
                pix);
    }
}
