package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class PosSalePaymentTotalMismatchException extends LojappDomainException {
    public PosSalePaymentTotalMismatchException() {
        super(ApiErrorCode.BAD_REQUEST, "Soma dos pagamentos difere do total da venda no PDV.");
    }
}
