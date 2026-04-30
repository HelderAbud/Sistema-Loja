package com.lojapp.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogRetentionServiceTest {

    @Mock private AuditLogRepository auditLogs;

    @Test
    void purgeExpiredAuditLogs_whenRetentionDisabled_doesNothing() {
        var service = new AuditLogRetentionService(auditLogs, 0);

        service.purgeExpiredAuditLogs();

        verify(auditLogs, never()).deleteByCreatedAtBefore(any());
    }

    @Test
    void purgeExpiredAuditLogs_whenRetentionEnabled_deletesOldRows() {
        var service = new AuditLogRetentionService(auditLogs, 180);
        when(auditLogs.deleteByCreatedAtBefore(any())).thenReturn(3);

        service.purgeExpiredAuditLogs();

        verify(auditLogs).deleteByCreatedAtBefore(any());
    }
}
