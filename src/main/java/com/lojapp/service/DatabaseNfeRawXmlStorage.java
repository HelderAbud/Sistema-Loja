package com.lojapp.service;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "lojapp.nfe.storage.backend",
        havingValue = "database",
        matchIfMissing = true)
public class DatabaseNfeRawXmlStorage implements NfeRawXmlStorage {

    @Override
    public StoredRawXml persist(long userId, String rawXml) {
        return new StoredRawXml(rawXml, null);
    }

    @Override
    public String retrieve(String rawXml, String rawXmlKey) {
        if (rawXml == null || rawXml.isBlank()) {
            throw new LojappDomainException(
                    ApiErrorCode.INTERNAL_ERROR,
                    "XML bruto da NFe não está disponível para esta entrada");
        }
        return rawXml;
    }
}
