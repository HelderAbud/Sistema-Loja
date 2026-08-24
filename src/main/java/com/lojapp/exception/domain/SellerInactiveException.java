package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class SellerInactiveException extends LojappDomainException {
    public SellerInactiveException() {
        super(ApiErrorCode.BAD_REQUEST, "Vendedora inactiva");
    }
}
