package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class CashSessionManagerApprovalRequiredException extends LojappDomainException {
    public CashSessionManagerApprovalRequiredException() {
        super(
                ApiErrorCode.FORBIDDEN,
                "Diferença acima da tolerância exige confirmação de revisão do valor contado.");
    }
}
