package com.lojapp.application.cash;

import com.lojapp.application.contract.CloseCashSessionUseCaseContract;
import com.lojapp.config.PosProperties;
import com.lojapp.dto.cash.CloseCashSessionRequest;
import com.lojapp.dto.cash.CloseCashSessionResponse;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.exception.domain.CashSessionDifferenceReasonRequiredException;
import com.lojapp.exception.domain.CashSessionManagerApprovalRequiredException;
import com.lojapp.exception.domain.CashSessionNotFoundException;
import com.lojapp.exception.domain.CashSessionNotOpenException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.AuditService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseCashSessionUseCase implements CloseCashSessionUseCaseContract {

    private final CashSessionRepository cashSessions;
    private final SalePaymentRepository salePayments;
    private final UserRepository users;
    private final AuditService auditService;
    private final PosProperties posProperties;

    public CloseCashSessionUseCase(
            CashSessionRepository cashSessions,
            SalePaymentRepository salePayments,
            UserRepository users,
            AuditService auditService,
            PosProperties posProperties) {
        this.cashSessions = cashSessions;
        this.salePayments = salePayments;
        this.users = users;
        this.auditService = auditService;
        this.posProperties = posProperties;
    }

    @Transactional
    public CloseCashSessionResponse execute(
            long userId, long actorUserId, CloseCashSessionRequest request) {
        CashSession cashSession =
                cashSessions
                        .findByIdAndUser_Id(request.cashSessionId(), userId)
                        .orElseThrow(CashSessionNotFoundException::new);

        if (cashSession.getStatus() != CashSessionStatus.OPEN) {
            throw new CashSessionNotOpenException();
        }

        BigDecimal expectedAmount = salePayments.sumAmountByCashSession(userId, cashSession.getId());
        BigDecimal differenceAmount = request.countedAmount().subtract(expectedAmount);

        if (differenceAmount.compareTo(BigDecimal.ZERO) != 0
                && (request.differenceReason() == null || request.differenceReason().isBlank())) {
            throw new CashSessionDifferenceReasonRequiredException();
        }

        if (differenceAmount.abs().compareTo(posProperties.closeToleranceAmount()) > 0
                && !request.managerApproval()) {
            throw new CashSessionManagerApprovalRequiredException();
        }

        cashSession.setExpectedAmount(expectedAmount);
        cashSession.setCountedAmount(request.countedAmount());
        cashSession.setDifferenceAmount(differenceAmount);
        cashSession.setDifferenceReason(normalizeReason(request.differenceReason()));
        cashSession.setClosedByUser(users.getReferenceById(actorUserId));
        cashSession.setClosedAt(java.time.Instant.now());
        cashSession.setStatus(CashSessionStatus.CLOSED);
        cashSessions.save(cashSession);

        auditService.log(
                userId,
                "CASH_SESSION_CLOSED",
                "cashSessionId=%d actorUserId=%d expectedAmount=%s countedAmount=%s differenceAmount=%s managerApproval=%s"
                        .formatted(
                                cashSession.getId(),
                                actorUserId,
                                expectedAmount,
                                request.countedAmount(),
                                differenceAmount,
                                request.managerApproval()));

        return new CloseCashSessionResponse(
                cashSession.getId(),
                expectedAmount,
                request.countedAmount(),
                differenceAmount,
                cashSession.getClosedAt(),
                cashSession.getStatus().name());
    }

    private static String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
