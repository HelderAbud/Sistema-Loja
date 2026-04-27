package com.lojapp.dto.dashboard;

import java.time.Instant;
import java.util.List;

public record BrandDashboardResponse(
        Instant from,
        Instant to,
        List<BrandKpiResponse> metrics,
        int totalBrands,
        int brandLimit,
        int brandOffset) {}
