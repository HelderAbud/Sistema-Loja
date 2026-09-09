package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class PosSalePaymentDetailsInvalidException extends LojappDomainException {
    public PosSalePaymentDetailsInvalidException(String message) {
        super(ApiErrorCode.BAD_REQUEST, message);
    }
}
