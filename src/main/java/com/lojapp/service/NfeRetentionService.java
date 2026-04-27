package com.lojapp.service;

import com.lojapp.repository.NfeEntryRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NfeRetentionService {

    private static final Logger log = LoggerFactory.getLogger(NfeRetentionService.class);

    private final NfeEntryRepository nfeEntries;
    private final int rawXmlRetentionDays;

    public NfeRetentionService(
            NfeEntryRepository nfeEntries,
            @Value("${lojapp.nfe.import.raw-xml-retention-days:30}") int rawXmlRetentionDays) {
        this.nfeEntries = nfeEntries;
        this.rawXmlRetentionDays = rawXmlRetentionDays;
    }

    @Scheduled(cron = "${lojapp.nfe.import.raw-xml-retention-cron:0 15 2 * * *}")
    @Transactional
    public void clearExpiredRawXml() {
        if (rawXmlRetentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(rawXmlRetentionDays, ChronoUnit.DAYS);
        int updated = nfeEntries.clearRawXmlOlderThan(cutoff);
        if (updated > 0) {
            log.info(
                    "Retencao NFe: raw_xml removido em {} entradas (cutoff={} dias)",
                    updated,
                    rawXmlRetentionDays);
        }
    }
}
