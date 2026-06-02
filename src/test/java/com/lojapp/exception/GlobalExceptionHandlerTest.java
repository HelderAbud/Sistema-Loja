package com.lojapp.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.sql.SQLException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void conflictUserMessage_foreignKey_returnsReferenceMessage() {
        var ex =
                new DataIntegrityViolationException(
                        "stmt failed", new SQLException("fk violation", "23503"));
        assertThat(GlobalExceptionHandler.conflictUserMessage(ex))
                .contains("Referência inválida");
    }

    @Test
    void conflictUserMessage_uniqueEmail_returnsEmailHint() {
        var ex =
                new DataIntegrityViolationException(
                        "could not execute statement",
                        new SQLException(
                                "duplicate key value violates unique constraint \"users_email_key\"",
                                "23505"));
        assertThat(GlobalExceptionHandler.conflictUserMessage(ex))
                .contains("email")
                .contains("Já existe");
    }

    @Test
    void conflictUserMessage_uniqueNonEmail_returnsGenericConflict() {
        var ex =
                new DataIntegrityViolationException(
                        "could not execute statement",
                        new SQLException(
                                "duplicate key value violates unique constraint \"products_sku_key\"",
                                "23505"));
        assertThat(GlobalExceptionHandler.conflictUserMessage(ex))
                .contains("conflito")
                .doesNotContain("email");
    }

    @Test
    void conflictUserMessage_withoutSqlState_usesTextualFallback() {
        var ex =
                new DataIntegrityViolationException(
                        "stmt failed",
                        new RuntimeException(
                                "ERROR: insert or update on table \"x\" violates foreign key constraint \"fk_y\""));
        assertThat(GlobalExceptionHandler.conflictUserMessage(ex))
                .contains("Referência inválida");
    }

    @Test
    void handleDataIntegrity_returnsConflictWithResolvedMessage() {
        var ex =
                new DataIntegrityViolationException(
                        "wrap",
                        new RuntimeException("ERROR: duplicate key value violates unique constraint \"uk_code\""));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/test");

        ResponseEntity<ApiErrorResponse> res = handler.handleDataIntegrity(ex, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().code()).isEqualTo(ApiErrorCode.CONFLICT.code());
        assertThat(res.getBody().message())
                .isEqualTo(
                        "Não foi possível guardar os dados devido a um conflito na base de dados (unicidade ou referência).");
    }

    @Test
    void handleConstraintViolation_returns400WithParameterName() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("dashboardByBrand.brandOffset");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be greater than or equal to 0");

        var ex = new ConstraintViolationException(Set.of(violation));
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/lojapp/dashboard/brands");

        ResponseEntity<ApiErrorResponse> res = handler.handleConstraintViolation(ex, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().code()).isEqualTo(ApiErrorCode.VALIDATION_ERROR.code());
        assertThat(res.getBody().message()).contains("brandOffset");
    }
}
