package com.lojapp.service;

import com.lojapp.config.IdempotencyProperties;
import com.lojapp.repository.ApiIdempotencyRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyCleanupService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupService.class);

    private final ApiIdempotencyRepository repository;
    private final IdempotencyProperties properties;

    public IdempotencyCleanupService(
            ApiIdempotencyRepository repository, IdempotencyProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(cron = "${lojapp.idempotency.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpiredRows() {
        long hours = Math.max(1, properties.ttlHours());
        Instant cutoff = Instant.now().minus(Duration.ofHours(hours)).minus(Duration.ofHours(1));
        int removed = repository.deleteByCreatedAtBefore(cutoff);
        if (removed > 0) {
            log.info("Limpeza api_idempotency: removidas {} linhas anteriores a {}", removed, cutoff);
        }
    }
}
