package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class CashSessionNotFoundException extends LojappDomainException {
    public CashSessionNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Turno de caixa não encontrado.");
    }
}
