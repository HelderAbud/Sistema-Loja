package com.lojapp.dto.product;

import com.lojapp.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Produto no catálogo (leitura)")
public record ProductResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Caderno A4") String name,
        @Schema(description = "Nome da marca ou texto fixo se não houver marca", example = "Marca X")
        String brandName,
        @Schema(example = "7891234567890") String ean,
        String ncm,
        String sku,
        BigDecimal costPrice,
        BigDecimal salePrice,
        BigDecimal minimumStock,
        @Schema(description = "Criação (auditoria)") Instant createdAt,
        @Schema(description = "ltima atualização (auditoria)") Instant updatedAt,
        @Schema(description = "Fornecedor preferido (id)", example = "1") Long supplierId,
        @Schema(description = "Modelo de catálogo (id)", example = "1") Long productModelId,
        String variantColor,
        String variantSize) {
    public static ProductResponse from(Product product) {
        String brandName =
                product.getBrand() == null ? "Nao informada" : product.getBrand().getName();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                brandName,
                product.getEan(),
                product.getNcm(),
                product.getSku(),
                product.getCostPrice(),
                product.getSalePrice(),
                product.getMinimumStock(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getSupplier() == null ? null : product.getSupplier().getId(),
                product.getProductModel() == null ? null : product.getProductModel().getId(),
                product.getVariantColor(),
                product.getVariantSize());
    }
}
