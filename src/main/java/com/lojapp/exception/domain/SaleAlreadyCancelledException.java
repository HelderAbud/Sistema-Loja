package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class SaleAlreadyCancelledException extends LojappDomainException {
    public SaleAlreadyCancelledException() {
        super(ApiErrorCode.CONFLICT, "Esta venda já foi cancelada");
    }
}
