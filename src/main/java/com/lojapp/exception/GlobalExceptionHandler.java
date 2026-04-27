package com.lojapp.exception;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.ApiErrorResponse;
import com.lojapp.exception.domain.LojappDomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AuthorizationDeniedException ex, HttpServletRequest req) {
        log.warn("Acesso negado em {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN, "Acesso negado", req);
    }

    @ExceptionHandler(LojappDomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomain(
            LojappDomainException ex, HttpServletRequest req) {
        HttpStatus status = httpStatusFor(ex.getErrorCode());
        if (status.is4xxClientError()) {
            log.warn(
                    "Regra de negócio em {}: [{}] {}",
                    req.getRequestURI(),
                    ex.getErrorCode(),
                    ex.getMessage());
        } else {
            log.error("Erro de domínio em {}", req.getRequestURI(), ex);
        }
        return build(status, ex.getErrorCode(), ex.getMessage(), req);
    }

    private static HttpStatus httpStatusFor(ApiErrorCode c) {
        return switch (c) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, DUPLICATE_NFE_XML -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case VALIDATION_ERROR, BAD_REQUEST -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String msg = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        ApiErrorCode code = ApiErrorCode.fromHttpStatus(status);
        return build(status, code, msg, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        return build(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                msg.isEmpty() ? "Dados inválidos" : msg,
                req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpServletRequest req) {
        return build(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.BAD_REQUEST,
                "Corpo JSON inválido ou incompatível",
                req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(
                HttpStatus.CONFLICT,
                ApiErrorCode.CONFLICT,
                conflictUserMessage(ex),
                req);
    }

    /**
     * Mensagem segura para o cliente: não expor SQL; contextualizar só quando a cadeia de causas
     * indica claramente tipo de violação (ex.: FK vs unique relacionado a email).
     */
    static String conflictUserMessage(DataIntegrityViolationException ex) {
        String chain = causeChainMessage(ex).toLowerCase(Locale.ROOT);
        if (chain.contains("foreign key") || chain.contains("foreign_key")) {
            return "Referência inválida: o registo depende de outro que não existe ou não pode ser alterado.";
        }
        boolean uniqueViolation =
                chain.contains("unique")
                        || chain.contains("duplicate")
                        || chain.contains("already exists")
                        || chain.contains("_key");
        boolean emailField = chain.contains("email");
        if (uniqueViolation && emailField) {
            return "Já existe um registo com este email (ou outro campo único em conflito).";
        }
        return "Não foi possível guardar os dados devido a um conflito na base de dados (unicidade ou referência).";
    }

    private static String causeChainMessage(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth++ < 8) {
            if (cur.getMessage() != null) {
                sb.append(' ').append(cur.getMessage());
            }
            cur = cur.getCause();
        }
        return sb.toString();
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccess(
            DataAccessException ex, HttpServletRequest req) {
        log.error("Erro de base de dados em {}", req.getRequestURI(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_ERROR,
                "Erro interno do servidor",
                req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro não tratado em {}", req.getRequestURI(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_ERROR,
                "Erro interno do servidor",
                req);
    }

    private static ResponseEntity<ApiErrorResponse> build(
            HttpStatus status, ApiErrorCode code, String message, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(
                        new ApiErrorResponse(
                                message,
                                code.code(),
                                status.value(),
                                req.getRequestURI(),
                                Instant.now()));
    }
}
