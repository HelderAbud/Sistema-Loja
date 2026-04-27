package com.lojapp.service;

import com.lojapp.entity.AuditLog;
import com.lojapp.repository.AuditLogRepository;
import com.lojapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogs;
    private final UserRepository users;

    public AuditService(AuditLogRepository auditLogs, UserRepository users) {
        this.auditLogs = auditLogs;
        this.users = users;
    }

    @Transactional
    public void log(Long userId, String action, String detail) {
        AuditLog row = new AuditLog();
        row.setAction(action);
        row.setDetail(detail);
        if (userId != null) {
            row.setUser(users.getReferenceById(userId));
        }
        auditLogs.save(row);
    }
}
