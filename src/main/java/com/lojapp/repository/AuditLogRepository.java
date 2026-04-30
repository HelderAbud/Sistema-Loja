package com.lojapp.repository;

import com.lojapp.entity.AuditLog;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    int deleteByCreatedAtBefore(Instant cutoff);
}
