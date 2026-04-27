package com.lojapp.dto.sale;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesDailyPointResponse(LocalDate date, BigDecimal revenue, BigDecimal unitsSold) {}
