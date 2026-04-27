package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class InsufficientStockException extends LojappDomainException {
    public InsufficientStockException() {
        super(ApiErrorCode.BAD_REQUEST, "Stock insuficiente para a quantidade pedida");
    }

    /** Mensagem explícita (ex.: ajuste manual que deixaria saldo negativo). */
    public InsufficientStockException(String message) {
        super(ApiErrorCode.BAD_REQUEST, message);
    }
}
