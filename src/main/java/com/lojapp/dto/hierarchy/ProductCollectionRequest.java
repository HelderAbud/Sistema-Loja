package com.lojapp.dto.hierarchy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Criar coleção sob uma marca")
public record ProductCollectionRequest(
        @NotNull @Positive @Schema(example = "1") Long brandId,
        @NotBlank @Size(max = 200) @Schema(example = "Verão 2026") String name) {}
