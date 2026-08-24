package com.lojapp.dto.seller;

import java.time.Instant;

public record SellerResponse(
        Long id, String displayName, boolean active, int sortOrder, Instant createdAt) {}
