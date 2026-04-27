package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class ProductCollectionNotFoundException extends LojappDomainException {
    public ProductCollectionNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Coleção não encontrada");
    }
}
