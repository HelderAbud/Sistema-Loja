package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class BrandNotFoundException extends LojappDomainException {
    public BrandNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Marca não encontrada");
    }
}
