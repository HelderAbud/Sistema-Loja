package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

/** Cancelamento recusado: o turno de caixa da venda já foi fechado. */
public final class SaleCashSessionAlreadyClosedException extends LojappDomainException {
    public SaleCashSessionAlreadyClosedException() {
        super(
                ApiErrorCode.CONFLICT,
                "Não é possível cancelar uma venda depois que o turno de caixa foi fechado");
    }
}
