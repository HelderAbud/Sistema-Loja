package com.lojapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

/**
 * Códigos estáveis devolvidos no campo {@code code} de {@link ApiErrorResponse}. O cliente pode
 * fazer ramificação por código em vez de analisar a mensagem.
 */
@Schema(
        description = "Código do tipo de erro (sempre um destes valores).",
        allowableValues = {
            "VALIDATION_ERROR",
            "BAD_REQUEST",
            "UNAUTHORIZED",
            "FORBIDDEN",
            "INTERNAL_ERROR",
            "CONFLICT",
            "NOT_FOUND",
            "DUPLICATE_NFE_XML"
        })
public enum ApiErrorCode {
    VALIDATION_ERROR,
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    INTERNAL_ERROR,
    CONFLICT,
    NOT_FOUND,
    /** Segunda importação do mesmo XML sem chave de acesso (dedupe por hash do conteúdo). */
    DUPLICATE_NFE_XML;

    public String code() {
        return name();
    }

    /** Mapeia um estado HTTP para o código de erro da API (fallback seguro para 4xx/5xx). */
    public static ApiErrorCode fromHttpStatus(HttpStatus status) {
        if (status == null) {
            return INTERNAL_ERROR;
        }
        return switch (status) {
            case BAD_REQUEST, UNSUPPORTED_MEDIA_TYPE, METHOD_NOT_ALLOWED -> BAD_REQUEST;
            case UNAUTHORIZED -> UNAUTHORIZED;
            case FORBIDDEN -> FORBIDDEN;
            case NOT_FOUND -> NOT_FOUND;
            case CONFLICT -> CONFLICT;
            case TOO_MANY_REQUESTS -> BAD_REQUEST;
            case INTERNAL_SERVER_ERROR, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT ->
                    INTERNAL_ERROR;
            default ->
                    status.is4xxClientError() ? BAD_REQUEST : INTERNAL_ERROR;
        };
    }
}
