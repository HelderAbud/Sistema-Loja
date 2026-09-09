package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class SalePaymentConfirmSessionClosedException extends LojappDomainException {
    public SalePaymentConfirmSessionClosedException() {
        super(
                ApiErrorCode.CONFLICT,
                "Abra o turno de caixa para confirmar a liquidação. O valor entra no caixa aberto, mesmo que a venda seja de um turno anterior.");
    }
}
