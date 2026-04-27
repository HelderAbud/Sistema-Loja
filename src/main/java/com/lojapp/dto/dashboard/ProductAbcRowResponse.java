package com.lojapp.dto.dashboard;

import java.math.BigDecimal;

public record ProductAbcRowResponse(
        long productId,
        String productName,
        String brandName,
        BigDecimal revenue,
        BigDecimal quantitySold,
        BigDecimal sharePercent,
        BigDecimal cumulativePercent,
        String abcClass) {}
