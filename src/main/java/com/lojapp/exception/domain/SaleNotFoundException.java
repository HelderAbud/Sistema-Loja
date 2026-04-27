package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class SaleNotFoundException extends LojappDomainException {
    public SaleNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Venda não encontrada");
    }
}
