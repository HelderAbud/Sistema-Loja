package com.lojapp.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.ApiErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class LojappErrorControllerTest {

    private final LojappErrorController controller = new LojappErrorController();

    @Test
    void jsonError_internalServerError_doesNotExposeExceptionMessage() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);
        when(req.getAttribute(RequestDispatcher.ERROR_EXCEPTION))
                .thenReturn(new RuntimeException("relation \"foo\" does not exist"));
        when(req.getAttribute(RequestDispatcher.ERROR_MESSAGE))
                .thenReturn("relation \"foo\" does not exist");
        when(req.getRequestURI()).thenReturn("/api/v1/products");

        ResponseEntity<ApiErrorResponse> res = controller.jsonError(req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().message()).isEqualTo(LojappErrorController.INTERNAL_CLIENT_MESSAGE);
        assertThat(res.getBody().message()).doesNotContain("relation");
        assertThat(res.getBody().message()).doesNotContain("foo");
        assertThat(res.getBody().code()).isEqualTo(ApiErrorCode.INTERNAL_ERROR.code());
    }

    @Test
    void jsonError_notFound_returnsSafeMessageDespiteSensitiveException() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(404);
        when(req.getAttribute(RequestDispatcher.ERROR_EXCEPTION))
                .thenReturn(new RuntimeException("No static resource secret/internal/path"));
        when(req.getRequestURI()).thenReturn("/secret/internal/path");

        ResponseEntity<ApiErrorResponse> res = controller.jsonError(req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().message()).isEqualTo("Recurso não encontrado");
        assertThat(res.getBody().message()).doesNotContain("secret");
    }

    @Test
    void jsonError_forbidden_returnsAccessDeniedMessage() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(403);
        when(req.getRequestURI()).thenReturn("/api/v1/admin");

        ResponseEntity<ApiErrorResponse> res = controller.jsonError(req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().message()).isEqualTo("Acesso negado");
        assertThat(res.getBody().code()).isEqualTo(ApiErrorCode.FORBIDDEN.code());
    }

    @Test
    void buildSafeClientMessage_alignsWithGlobalExceptionHandlerFor5xx() {
        assertThat(LojappErrorController.buildSafeClientMessage(HttpStatus.INTERNAL_SERVER_ERROR))
                .isEqualTo("Erro interno do servidor");
    }
}
