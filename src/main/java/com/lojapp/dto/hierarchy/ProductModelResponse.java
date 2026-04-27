package com.lojapp.dto.hierarchy;

import com.lojapp.entity.ProductModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Modelo de produto (leitura)")
public record ProductModelResponse(
        Long id,
        Long brandId,
        String brandName,
        Long collectionId,
        @Schema(description = "Nome da coleção, se houver") String collectionName,
        String name,
        Instant createdAt,
        Instant updatedAt) {
    public static ProductModelResponse from(ProductModel m) {
        Long cid = null;
        String cname = null;
        if (m.getCollection() != null) {
            cid = m.getCollection().getId();
            cname = m.getCollection().getName();
        }
        return new ProductModelResponse(
                m.getId(),
                m.getBrand().getId(),
                m.getBrand().getName(),
                cid,
                cname,
                m.getName(),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }
}
