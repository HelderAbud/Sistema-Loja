package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class NfeEntryNotFoundException extends LojappDomainException {
    public NfeEntryNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Entrada de NFe não encontrada");
    }
}
