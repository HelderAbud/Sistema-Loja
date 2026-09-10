package com.lojapp.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(
        description =
                "Página de movimentos. A lista está em `content`; metadados de paginação nos outros campos.")
public record InventoryMovementPageResponse(
        @Schema(description = "Itens da página atual") List<InventoryMovementResponse> content,
        @Schema(description = "Total de elementos em todas as páginas", example = "42")
        long totalElements,
        @Schema(description = "Número total de páginas", example = "3") int totalPages,
        @Schema(description = "Tamanho pedido da página", example = "20") int size,
        @Schema(description = "Índice da página (0-based)", example = "0") int number,
        @Schema(description = "Se é a primeira página") boolean first,
        @Schema(description = "Se é a última página") boolean last) {

    public static InventoryMovementPageResponse from(Page<InventoryMovementResponse> page) {
        return new InventoryMovementPageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast());
    }
}
