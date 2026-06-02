package com.lojapp.exception;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.ApiErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Substitui o {@code BasicErrorController} quando presente (via
 * {@code @ConditionalOnMissingBean(ErrorController.class)}), garantindo corpo JSON em erros que
 * não passam pelo {@link GlobalExceptionHandler} (ex.: falhas na cadeia de filtros).
 *
 * <p>Mensagens ao cliente alinham-se a {@link GlobalExceptionHandler}: detalhe interno só em logs.
 */
@RestController
public class LojappErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(LojappErrorController.class);

    /** Mesma mensagem genérica que {@link GlobalExceptionHandler} usa em 5xx. */
    static final String INTERNAL_CLIENT_MESSAGE = "Erro interno do servidor";

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ApiErrorResponse> jsonError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable ex = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int code = statusCode != null ? statusCode : 500;
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        logServerSide(status, ex, request);

        String msg = buildSafeClientMessage(status);
        ApiErrorCode apiCode = ApiErrorCode.fromHttpStatus(status);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        new ApiErrorResponse(
                                msg,
                                apiCode.code(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now()));
    }

    private static void logServerSide(
            HttpStatus status, Throwable ex, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (status.is5xxServerError()) {
            if (ex != null) {
                log.error("Erro /error {} (HTTP {}):", uri, status.value(), ex);
            } else {
                Object dispatcherMsg = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
                log.error(
                        "Erro /error {} (HTTP {}) sem exceção; dispatcherMessage={}",
                        uri,
                        status.value(),
                        dispatcherMsg);
            }
            return;
        }
        if (ex != null) {
            log.warn("Erro cliente /error {} (HTTP {}): {}", uri, status.value(), ex.toString(), ex);
        }
    }

    /**
     * Mensagem segura para o cliente. Nunca expõe {@code Throwable.getMessage()},
     * {@link RequestDispatcher#ERROR_MESSAGE} nem nomes de classes internas.
     */
    static String buildSafeClientMessage(HttpStatus status) {
        if (status.is5xxServerError()) {
            return INTERNAL_CLIENT_MESSAGE;
        }
        return switch (status) {
            case BAD_REQUEST -> "Pedido inválido";
            case UNAUTHORIZED -> "Não autenticado";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Recurso não encontrado";
            case METHOD_NOT_ALLOWED -> "Método não permitido";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de conteúdo não suportado";
            case TOO_MANY_REQUESTS -> "Muitos pedidos; tente novamente mais tarde";
            default -> {
                String phrase = status.getReasonPhrase();
                yield phrase != null && !phrase.isBlank()
                        ? phrase
                        : "Pedido não pode ser processado";
            }
        };
    }
}
