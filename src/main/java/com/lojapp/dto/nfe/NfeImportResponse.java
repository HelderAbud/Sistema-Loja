package com.lojapp.dto.nfe;

import io.swagger.v3.oas.annotations.media.Schema;

public record NfeImportResponse(
        Long nfeEntryId,
        String nfeNumber,
        int importedItems,
        @Schema(description = "Fornecedor deduplicado pelo CNPJ/CPF do emitente; null se a nota não trouxe identificação.")
                Long supplierId,
        @Schema(
                        description =
                                "Sugestão de marca (heurística por texto do emitente/itens); aplicar só após confirmação humana.")
                Long suggestedBrandId,
        @Schema(description = "Nome da marca sugerida (espelho de suggestedBrandId).") String suggestedBrandName,
        @Schema(
                        description =
                                "Quantidade de produtos criados automaticamente na importação sem preço de venda "
                                        + "definido (preço de venda = custo até revisão manual).")
                int productsCreatedWithoutSalePrice) {}
