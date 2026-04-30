package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class CashSessionNotOpenException extends LojappDomainException {
    public CashSessionNotOpenException() {
        super(ApiErrorCode.CONFLICT, "O turno de caixa informado não está aberto.");
    }
}
