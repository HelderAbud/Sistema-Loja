package com.lojapp.dto.inventory;

import java.math.BigDecimal;

public record LowStockResponse(
        Long productId, String productName, BigDecimal currentQuantity, BigDecimal minimumStock) {}
