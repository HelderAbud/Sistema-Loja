package com.lojapp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Formato padrão de erro da API")
@JsonPropertyOrder({"message", "code", "status", "path", "timestamp"})
public record ApiErrorResponse(
        @Schema(description = "Mensagem legível para o utilizador ou cliente") String message,
        @Schema(
                        description = "Código estável do tipo de erro; ver ApiErrorCode.",
                        example = "VALIDATION_ERROR")
                String code,
        @Schema(description = "HTTP status numérico", example = "400") int status,
        @Schema(description = "Path da requisição", example = "/api/v1/lojapp/products") String path,
        @Schema(description = "Instante ISO-8601 (UTC)") Instant timestamp) {}
