package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class DuplicateNfeAccessKeyException extends LojappDomainException {
    public DuplicateNfeAccessKeyException() {
        super(ApiErrorCode.CONFLICT, "NFe com esta chave de acesso já foi importada");
    }
}
