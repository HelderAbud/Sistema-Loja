package com.lojapp.dto.hierarchy;

import com.lojapp.entity.ProductCollection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Coleção de produtos (leitura)")
public record ProductCollectionResponse(
        Long id,
        Long brandId,
        @Schema(description = "Nome da marca") String brandName,
        String name,
        Instant createdAt,
        Instant updatedAt) {
    public static ProductCollectionResponse from(ProductCollection c) {
        return new ProductCollectionResponse(
                c.getId(),
                c.getBrand().getId(),
                c.getBrand().getName(),
                c.getName(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
