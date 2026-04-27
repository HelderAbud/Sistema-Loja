package com.lojapp.dto.dashboard;

import java.math.BigDecimal;

public record InventoryKpiResponse(
        int totalSkus, BigDecimal totalUnits, int lowStockCount, int skusWithPositiveStock) {}
