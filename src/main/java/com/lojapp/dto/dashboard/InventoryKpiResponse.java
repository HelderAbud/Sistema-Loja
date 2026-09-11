package com.lojapp.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "KPIs de estoque da loja autenticada.")
public record InventoryKpiResponse(
        int totalSkus,
        BigDecimal totalUnits,
        int lowStockCount,
        int skusWithPositiveStock,
        @Schema(description = "Soma de custo × quantidade (saldo actual)", example = "1500.00")
                BigDecimal totalStockValue) {}
