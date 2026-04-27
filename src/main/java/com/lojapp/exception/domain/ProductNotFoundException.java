package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class ProductNotFoundException extends LojappDomainException {
    public ProductNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Produto não encontrado");
    }
}
