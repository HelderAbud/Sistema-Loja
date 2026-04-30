package com.lojapp.application.cash;

import com.lojapp.application.contract.OpenCashSessionUseCaseContract;
import com.lojapp.dto.cash.OpenCashSessionRequest;
import com.lojapp.dto.cash.OpenCashSessionResponse;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.exception.domain.CashSessionAlreadyOpenException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenCashSessionUseCase implements OpenCashSessionUseCaseContract {

    private final CashSessionRepository cashSessions;
    private final UserRepository users;
    private final AuditService auditService;

    public OpenCashSessionUseCase(
            CashSessionRepository cashSessions, UserRepository users, AuditService auditService) {
        this.cashSessions = cashSessions;
        this.users = users;
        this.auditService = auditService;
    }

    @Transactional
    public OpenCashSessionResponse execute(long userId, long actorUserId, OpenCashSessionRequest request) {
        if (cashSessions.findByUser_IdAndStatus(userId, CashSessionStatus.OPEN).isPresent()) {
            throw new CashSessionAlreadyOpenException();
        }

        CashSession cashSession = new CashSession();
        cashSession.setUser(users.getReferenceById(userId));
        cashSession.setOpenedByUser(users.getReferenceById(actorUserId));
        cashSession.setStatus(CashSessionStatus.OPEN);
        cashSession.setOpeningAmount(request.openingAmount());
        cashSessions.save(cashSession);

        auditService.log(
                userId,
                "CASH_SESSION_OPENED",
                "cashSessionId=%d actorUserId=%d openingAmount=%s"
                        .formatted(cashSession.getId(), actorUserId, cashSession.getOpeningAmount()));

        return new OpenCashSessionResponse(
                cashSession.getId(),
                cashSession.getOpeningAmount(),
                cashSession.getOpenedAt(),
                cashSession.getStatus().name());
    }
}
