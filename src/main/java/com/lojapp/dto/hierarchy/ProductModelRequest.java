package com.lojapp.dto.hierarchy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Criar modelo de produto sob uma marca")
public record ProductModelRequest(
        @NotNull @Positive @Schema(example = "1") Long brandId,
        @Schema(description = "Coleção opcional (deve pertencer à mesma marca)", example = "2")
                @Positive
                Long collectionId,
        @NotBlank @Size(max = 255) @Schema(example = "Camisa polo básica") String name) {}
