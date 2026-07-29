package com.lojapp.application.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.dto.cash.OpenCashSessionRequest;
import com.lojapp.dto.cash.OpenCashSessionResponse;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.CashSessionAlreadyOpenException;
import com.lojapp.repository.CashSessionRepository;
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
class OpenCashSessionUseCaseTest {

    @Mock private CashSessionRepository cashSessions;
    @Mock private UserRepository users;
    @Mock private AuditService auditService;

    private OpenCashSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new OpenCashSessionUseCase(cashSessions, users, auditService);
    }

    @Test
    void execute_whenAlreadyOpen_throws() {
        when(cashSessions.findByUser_IdAndStatus(1L, CashSessionStatus.OPEN))
                .thenReturn(Optional.of(new CashSession()));

        assertThatThrownBy(
                        () ->
                                useCase.execute(
                                        1L, 1L, new OpenCashSessionRequest(new BigDecimal("50.00"))))
                .isInstanceOf(CashSessionAlreadyOpenException.class);

        verify(cashSessions, never()).save(any());
    }

    @Test
    void execute_opensSessionAndAudits() {
        when(cashSessions.findByUser_IdAndStatus(1L, CashSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        User owner = new User();
        owner.setId(1L);
        when(users.getReferenceById(1L)).thenReturn(owner);
        when(cashSessions.save(any(CashSession.class)))
                .thenAnswer(
                        inv -> {
                            CashSession s = inv.getArgument(0);
                            s.setId(99L);
                            return s;
                        });

        OpenCashSessionResponse response =
                useCase.execute(1L, 1L, new OpenCashSessionRequest(new BigDecimal("50.00")));

        assertThat(response.cashSessionId()).isEqualTo(99L);
        assertThat(response.openingAmount()).isEqualByComparingTo("50.00");
        assertThat(response.status()).isEqualTo("OPEN");

        ArgumentCaptor<CashSession> captor = ArgumentCaptor.forClass(CashSession.class);
        verify(cashSessions).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CashSessionStatus.OPEN);
        verify(auditService).log(eq(1L), eq("CASH_SESSION_OPENED"), anyString());
    }
}
