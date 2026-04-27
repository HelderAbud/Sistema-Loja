package com.lojapp.service;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NfeImportValidator {

    private final int maxImportXmlChars;
    private final int maxImportItems;

    public NfeImportValidator(
            @Value("${lojapp.nfe.import.max-xml-chars:12000000}") int maxImportXmlChars,
            @Value("${lojapp.nfe.import.max-items:1000}") int maxImportItems) {
        this.maxImportXmlChars = maxImportXmlChars;
        this.maxImportItems = maxImportItems;
    }

    public void validateRawXml(String rawXml) {
        if (rawXml == null || rawXml.isBlank()) {
            throw new LojappDomainException(ApiErrorCode.BAD_REQUEST, "rawXml obrigatório");
        }
        if (rawXml.length() > maxImportXmlChars) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST,
                    "XML da NFe excede o limite de %d caracteres".formatted(maxImportXmlChars));
        }
    }

    public void validateParsedItems(int itemCount) {
        if (itemCount <= 0) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST, "NFe sem itens válidos para importação");
        }
        if (itemCount > maxImportItems) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST,
                    "NFe excede o limite de %d itens".formatted(maxImportItems));
        }
    }
}
