package com.lojapp.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Criar ou atualizar produto")
public record ProductRequest(
        @NotBlank @Size(max = 255) @Schema(example = "Caderno A4") String name,
        @Positive @Schema(description = "Id da marca (opcional)", example = "1") Long brandId,
        @Size(max = 32) String ean,
        @Size(max = 12) String ncm,
        @Size(max = 64) String sku,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal costPrice,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal salePrice,
        @NotNull @DecimalMin("0.000") @Digits(integer = 16, fraction = 3) BigDecimal minimumStock,
        @Positive @Schema(description = "Fornecedor preferido do SKU (opcional)", example = "1") Long supplierId,
        @Schema(
                        description =
                                "Modelo de catálogo (opcional). Quando definido, a marca do produto alinha-se à marca do modelo.",
                        example = "1")
        @Positive Long productModelId,
        @Size(max = 64) @Schema(description = "Cor da variante", example = "Azul marinho") String variantColor,
        @Size(max = 32) @Schema(description = "Tamanho da variante", example = "M") String variantSize) {

    /** Compatível com payloads e testes que ainda não enviam hierarquia multimarcas. */
    public ProductRequest(
            String name,
            Long brandId,
            String ean,
            String ncm,
            String sku,
            BigDecimal costPrice,
            BigDecimal salePrice,
            BigDecimal minimumStock) {
        this(
                name,
                brandId,
                ean,
                ncm,
                sku,
                costPrice,
                salePrice,
                minimumStock,
                null,
                null,
                null,
                null);
    }
}
