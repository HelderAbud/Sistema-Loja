package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class CashSessionDifferenceReasonRequiredException extends LojappDomainException {
    public CashSessionDifferenceReasonRequiredException() {
        super(ApiErrorCode.BAD_REQUEST, "Motivo obrigatório para fechamento com diferença de caixa.");
    }
}
