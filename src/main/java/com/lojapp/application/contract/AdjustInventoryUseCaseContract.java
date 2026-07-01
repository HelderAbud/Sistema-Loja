package com.lojapp.application.contract;

import com.lojapp.dto.inventory.StockAdjustmentRequest;
import java.util.Optional;

public interface AdjustInventoryUseCaseContract {

    void execute(
            long userId, StockAdjustmentRequest request, Optional<String> idempotencyKeyHeader);
}
