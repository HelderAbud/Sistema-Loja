package com.lojapp.exception.domain;

import com.lojapp.dto.ApiErrorCode;

/** Erro de regra de negócio; mapeado para HTTP em {@link com.lojapp.exception.GlobalExceptionHandler}. */
public class LojappDomainException extends RuntimeException {

    private final ApiErrorCode errorCode;

    public LojappDomainException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LojappDomainException(ApiErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
