package com.lojapp.dto.inventory;

import java.math.BigDecimal;

/** Saldo disponível em {@code inventory_balances} para o par utilizador + produto. */
public record ProductStockResponse(BigDecimal quantity) {}
