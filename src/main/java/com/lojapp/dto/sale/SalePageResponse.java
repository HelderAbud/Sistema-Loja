package com.lojapp.dto.sale;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Página de vendas (lista em `content`).")
public record SalePageResponse(
        List<SaleListItemResponse> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last) {

    public static SalePageResponse from(Page<SaleListItemResponse> page) {
        return new SalePageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast());
    }
}
