package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

public final class NfeXmlUnreadableException extends LojappDomainException {
    public NfeXmlUnreadableException(String message, Throwable cause) {
        super(ApiErrorCode.BAD_REQUEST, message, cause);
    }
}
