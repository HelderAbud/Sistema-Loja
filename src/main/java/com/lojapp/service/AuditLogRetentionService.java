package com.lojapp.service;

import com.lojapp.repository.AuditLogRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionService.class);

    private final AuditLogRepository auditLogs;
    private final int retentionDays;

    public AuditLogRetentionService(
            AuditLogRepository auditLogs,
            @Value("${lojapp.audit.retention-days:180}") int retentionDays) {
        this.auditLogs = auditLogs;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${lojapp.audit.retention-cron:0 45 3 * * *}")
    @Transactional
    public void purgeExpiredAuditLogs() {
        if (retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int removed = auditLogs.deleteByCreatedAtBefore(cutoff);
        if (removed > 0) {
            log.info("Retencao audit_logs: removidas {} linhas anteriores a {}", removed, cutoff);
        }
    }
}
