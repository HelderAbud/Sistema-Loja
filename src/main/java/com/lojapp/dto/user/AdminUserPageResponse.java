package com.lojapp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Página de utilizadores para visão administrativa.")
public record AdminUserPageResponse(
        List<AdminUserSummaryResponse> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last) {

    public static AdminUserPageResponse from(Page<AdminUserSummaryResponse> page) {
        return new AdminUserPageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast());
    }
}
