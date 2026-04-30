package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class CashSessionAlreadyOpenException extends LojappDomainException {
    public CashSessionAlreadyOpenException() {
        super(ApiErrorCode.CONFLICT, "Já existe um turno de caixa aberto para esta loja.");
    }
}
