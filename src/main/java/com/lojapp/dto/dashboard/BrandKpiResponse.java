package com.lojapp.dto.dashboard;

import java.math.BigDecimal;

public record BrandKpiResponse(
        String brand,
        BigDecimal faturamento,
        BigDecimal lucro,
        BigDecimal quantidadeVendida,
        BigDecimal margem,
        String giro,
        String insight) {}
