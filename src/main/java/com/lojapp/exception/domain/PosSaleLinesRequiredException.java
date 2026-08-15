package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class PosSaleLinesRequiredException extends LojappDomainException {
    public PosSaleLinesRequiredException() {
        super(
                ApiErrorCode.BAD_REQUEST,
                "Informe items da venda PDV ou os campos de uma única linha (productId, quantity, unitPrice).");
    }
}
