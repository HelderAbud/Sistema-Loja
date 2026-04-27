package com.lojapp.dto.nfe;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corpo opcional: omissão ou null em cada campo ativa o comportamento por defeito (true).")
public record NfeApplySuggestionsRequest(
        @Schema(description = "Aplicar marca sugerida (re-lida do XML) em produtos desta NFe sem marca", example = "true")
                Boolean setBrandOnImportedProducts,
        @Schema(description = "Aplicar fornecedor da entrada em produtos sem fornecedor", example = "true")
                Boolean setSupplierOnImportedProducts) {}
