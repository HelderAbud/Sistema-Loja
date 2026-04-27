package com.lojapp.dto.sale;

import java.math.BigDecimal;

public record SalesSummaryResponse(BigDecimal revenue, BigDecimal unitsSold, BigDecimal averageTicket) {}
