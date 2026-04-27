package com.lojapp.dto.nfe;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de aplicar sugestões aos produtos ligados a uma NFe importada.")
public record NfeApplySuggestionsResponse(
        @Schema(description = "Número de linhas de produto analisadas (itens da NFe)") int nfeLineCount,
        @Schema(description = "Produtos em que a marca sugerida foi gravada") int brandAssignedCount,
        @Schema(description = "Produtos em que o fornecedor da entrada foi gravado") int supplierAssignedCount,
        @Schema(description = "Marcas ignoradas por conflito com modelo de catálogo") int brandSkippedModelConflictCount,
        Long appliedBrandId,
        String appliedBrandName,
        Long supplierIdFromEntry) {}
