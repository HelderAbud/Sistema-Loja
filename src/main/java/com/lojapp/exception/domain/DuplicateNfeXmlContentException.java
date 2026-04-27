package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

/** XML já importado anteriormente para o utilizador (sem chave de acesso; dedupe por hash do conteúdo). */
public final class DuplicateNfeXmlContentException extends LojappDomainException {
    public DuplicateNfeXmlContentException() {
        super(
                ApiErrorCode.DUPLICATE_NFE_XML,
                "Este XML já foi importado (sem chave de acesso no documento).");
    }
}
