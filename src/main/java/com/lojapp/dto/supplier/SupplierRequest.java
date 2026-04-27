package com.lojapp.dto.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Criar fornecedor")
public record SupplierRequest(
        @NotBlank @Size(max = 255) @Schema(example = "Distribuidora ABC Ltda") String legalName,
        @Size(max = 32)
                @Pattern(
                        regexp = "^[0-9.\\-/()\\s]*$",
                        message = "taxId deve conter apenas dígitos e máscaras comuns")
                @Schema(description = "CNPJ ou CPF (com ou sem máscara); opcional", example = "12.345.678/0001-99")
                String taxId) {}
