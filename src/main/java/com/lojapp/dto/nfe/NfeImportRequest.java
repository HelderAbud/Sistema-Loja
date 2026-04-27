package com.lojapp.dto.nfe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NfeImportRequest(
        @NotBlank @Size(max = 12_000_000) String rawXml) {}
