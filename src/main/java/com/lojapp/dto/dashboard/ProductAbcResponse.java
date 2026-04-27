package com.lojapp.dto.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductAbcResponse(
        Instant from, Instant to, BigDecimal totalRevenue, List<ProductAbcRowResponse> rows) {}
