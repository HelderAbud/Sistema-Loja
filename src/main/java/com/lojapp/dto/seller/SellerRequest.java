package com.lojapp.dto.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerRequest(
        @NotBlank @Size(max = 120) String displayName, Boolean active, Integer sortOrder) {}
