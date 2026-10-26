package com.lojapp.application.usecases.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.config.PosProperties;
import com.lojapp.dto.cash.CloseCashSessionRequest;
import com.lojapp.dto.cash.CloseCashSessionResponse;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.CashSessionDifferenceReasonRequiredException;
import com.lojapp.exception.domain.CashSessionManagerApprovalRequiredException;
import com.lojapp.exception.domain.CashSessionNotFoundException;
import com.lojapp.exception.domain.CashSessionNotOpenException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.AuditService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloseCashSessionUseCaseTest {

    private static final long STORE_USER_ID = 1L;
    private static final long ACTOR_USER_ID = 7L;
    private static final long SESSION_ID = 40L;
    private static final BigDecimal TOLERANCE = new BigDecimal("10.00");

    @Mock private CashSessionRepository cashSessions;
    @Mock private SalePaymentRepository salePayments;
    @Mock private UserRepository users;
    @Mock private AuditService auditService;

    private CloseCashSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase =
                new CloseCashSessionUseCase(
                        cashSessions, salePayments, users, auditService, new PosProperties(TOLERANCE));
    }

    @Test
    void execute_whenSessionMissing_throwsAndDoesNotClose() {
        when(cashSessions.findByIdAndUser_Id(SESSION_ID, STORE_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(STORE_USER_ID, ACTOR_USER_ID, request("100.00", null, false)))
                .isInstanceOf(CashSessionNotFoundException.class);

        verify(cashSessions, never()).save(any());
        verify(auditService, never()).log(anyLong(), anyString(), anyString());
    }

    @Test
    void execute_whenSessionAlreadyClosed_throwsAndDoesNotChangeAmounts() {
        CashSession closed = openSession();
        closed.setStatus(CashSessionStatus.CLOSED);
        when(cashSessions.findByIdAndUser_Id(SESSION_ID, STORE_USER_ID)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> useCase.execute(STORE_USER_ID, ACTOR_USER_ID, request("100.00", null, false)))
                .isInstanceOf(CashSessionNotOpenException.class);

        verify(salePayments, never()).sumAmountByCashSession(anyLong(), anyLong());
        verify(cashSessions, never()).save(any());
    }

    @Test
    void execute_whenCountedDiffersAndReasonIsBlank_throws() {
        stubOpenSessionExpected("100.00");

        assertThatThrownBy(() -> useCase.execute(STORE_USER_ID, ACTOR_USER_ID, request("90.00", "  ", false)))
                .isInstanceOf(CashSessionDifferenceReasonRequiredException.class);

        verify(cashSessions, never()).save(any());
    }

    @Test
    void execute_whenDifferenceExceedsToleranceWithoutApproval_throws() {
        stubOpenSessionExpected("100.00");

        assertThatThrownBy(
                        () ->
                                useCase.execute(
                                        STORE_USER_ID,
                                        ACTOR_USER_ID,
                                        request("80.00", "falta no gaveta", false)))
                .isInstanceOf(CashSessionManagerApprovalRequiredException.class);

        verify(cashSessions, never()).save(any());
    }

    @Test
    void execute_whenCountedMatchesExpected_closesWithoutReasonOrApproval() {
        stubOpenSessionExpected("100.00");
        User actor = actor();
        when(users.getReferenceById(ACTOR_USER_ID)).thenReturn(actor);

        CloseCashSessionResponse response =
                useCase.execute(STORE_USER_ID, ACTOR_USER_ID, request("100.00", null, false));

        assertThat(response.cashSessionId()).isEqualTo(SESSION_ID);
        assertThat(response.expectedAmount()).isEqualByComparingTo("100.00");
        assertThat(response.countedAmount()).isEqualByComparingTo("100.00");
        assertThat(response.differenceAmount()).isEqualByComparingTo("0.00");
        assertThat(response.closedAt()).isNotNull();
        assertThat(response.status()).isEqualTo("CLOSED");

        CashSession saved = savedSession();
        assertThat(saved.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(saved.getDifferenceReason()).isNull();
        assertThat(saved.getClosedByUser()).isSameAs(actor);
        verify(auditService).log(eq(STORE_USER_ID), eq("CASH_SESSION_CLOSED"), anyString());
    }

    @Test
    void execute_whenDifferenceEqualsTolerance_closesWithReasonAndWithoutApproval() {
        stubOpenSessionExpected("100.00");
        when(users.getReferenceById(ACTOR_USER_ID)).thenReturn(actor());

        CloseCashSessionResponse response =
                useCase.execute(STORE_USER_ID, ACTOR_USER_ID, request("110.00", "  troco a mais  ", false));

        assertThat(response.differenceAmount()).isEqualByComparingTo("10.00");
        CashSession saved = savedSession();
        assertThat(saved.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(saved.getDifferenceReason()).isEqualTo("troco a mais");
        assertThat(saved.getExpectedAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getCountedAmount()).isEqualByComparingTo("110.00");
    }

    @Test
    void execute_whenDifferenceExceedsToleranceWithReasonAndApproval_closes() {
        stubOpenSessionExpected("100.00");
        when(users.getReferenceById(ACTOR_USER_ID)).thenReturn(actor());

        CloseCashSessionResponse response =
                useCase.execute(
                        STORE_USER_ID, ACTOR_USER_ID, request("80.00", "sangria não registada", true));

        assertThat(response.differenceAmount()).isEqualByComparingTo("-20.00");
        assertThat(response.status()).isEqualTo("CLOSED");
        CashSession saved = savedSession();
        assertThat(saved.getDifferenceReason()).isEqualTo("sangria não registada");
        verify(auditService).log(eq(STORE_USER_ID), eq("CASH_SESSION_CLOSED"), anyString());
    }

    private void stubOpenSessionExpected(String expected) {
        when(cashSessions.findByIdAndUser_Id(SESSION_ID, STORE_USER_ID))
                .thenReturn(Optional.of(openSession()));
        when(salePayments.sumAmountByCashSession(STORE_USER_ID, SESSION_ID))
                .thenReturn(new BigDecimal(expected));
    }

    private CashSession openSession() {
        CashSession session = new CashSession();
        session.setId(SESSION_ID);
        session.setStatus(CashSessionStatus.OPEN);
        return session;
    }

    private User actor() {
        User actor = new User();
        actor.setId(ACTOR_USER_ID);
        return actor;
    }

    private CloseCashSessionRequest request(String counted, String reason, boolean managerApproval) {
        return new CloseCashSessionRequest(SESSION_ID, new BigDecimal(counted), reason, managerApproval);
    }

    private CashSession savedSession() {
        ArgumentCaptor<CashSession> captor = ArgumentCaptor.forClass(CashSession.class);
        verify(cashSessions).save(captor.capture());
        return captor.getValue();
    }
}
