package com.lojapp.dto.supplier;

import com.lojapp.entity.Supplier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Fornecedor (leitura)")
public record SupplierResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Distribuidora ABC Ltda") String legalName,
        @Schema(description = "CNPJ ou CPF normalizado (só dígitos), se informado") String taxId,
        Instant createdAt,
        Instant updatedAt) {
    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(
                s.getId(), s.getLegalName(), s.getCnpj(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
