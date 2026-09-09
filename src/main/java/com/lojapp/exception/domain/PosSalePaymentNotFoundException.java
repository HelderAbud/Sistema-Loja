package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class PosSalePaymentNotFoundException extends LojappDomainException {
    public PosSalePaymentNotFoundException() {
        super(ApiErrorCode.NOT_FOUND, "Pagamento não encontrado");
    }
}
