package com.lojapp.exception;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.ApiErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
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
 */
@RestController
public class LojappErrorController implements ErrorController {

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ApiErrorResponse> jsonError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable ex = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int code = statusCode != null ? statusCode : 500;
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String msg = buildMessage(ex, request);
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

    private static String buildMessage(Throwable ex, HttpServletRequest request) {
        if (ex != null) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String m = root.getMessage();
            if (m != null && !m.isBlank()) {
                return m.length() > 500 ? m.substring(0, 500) + "" : m;
            }
            return root.getClass().getSimpleName();
        }
        Object fallback = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (fallback != null && !String.valueOf(fallback).isBlank()) {
            return String.valueOf(fallback);
        }
        return "Erro no servidor sem detalhe adicional. Consulte os logs da API (nível ERROR).";
    }
}
