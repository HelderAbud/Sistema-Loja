package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class SellerNotFoundException extends LojappDomainException {
    public SellerNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Vendedora não encontrada");
    }
}
