package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class PosSaleDuplicateProductException extends LojappDomainException {
    public PosSaleDuplicateProductException() {
        super(ApiErrorCode.BAD_REQUEST, "A venda PDV não pode repetir o mesmo produto em linhas distintas.");
    }
}
