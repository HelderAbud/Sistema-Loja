package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class ProductModelNotFoundException extends LojappDomainException {
    public ProductModelNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Modelo de produto não encontrado");
    }
}
