package com.lojapp.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envelope estável para JSON (evita depender da serialização directa de {@link Page}).
 */
@Schema(
        description =
                "Página de produtos. A lista está em `content`; metadados de paginação nos outros campos.")
public record ProductPageResponse(
        @Schema(description = "Itens da página atual") List<ProductResponse> content,
        @Schema(description = "Total de elementos em todas as páginas", example = "42")
        long totalElements,
        @Schema(description = "Número total de páginas", example = "3") int totalPages,
        @Schema(description = "Tamanho pedido da página", example = "20") int size,
        @Schema(description = "Índice da página (0-based)", example = "0") int number,
        @Schema(description = "Se é a primeira página") boolean first,
        @Schema(description = "Se é a última página") boolean last) {

    public static ProductPageResponse from(Page<ProductResponse> page) {
        return new ProductPageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast());
    }
}
