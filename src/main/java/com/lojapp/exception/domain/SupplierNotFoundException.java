package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class SupplierNotFoundException extends LojappDomainException {
    public SupplierNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Fornecedor não encontrado");
    }
}
