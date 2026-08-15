package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class PosSaleTooManyLinesException extends LojappDomainException {
    public PosSaleTooManyLinesException(int maxLines) {
        super(ApiErrorCode.BAD_REQUEST, "A venda PDV aceita no máximo %d linhas.".formatted(maxLines));
    }
}
