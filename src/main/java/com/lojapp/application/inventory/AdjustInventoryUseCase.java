package com.lojapp.application.inventory;

import com.lojapp.application.idempotency.ApiIdempotencyService;
import com.lojapp.application.idempotency.RequestFingerprint;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.service.contract.InventoryServiceContract;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Caso de uso: ajuste manual de stock com suporte a {@code Idempotency-Key}.
 */
@Service
public class AdjustInventoryUseCase {

    private final ApiIdempotencyService idempotencyService;
    private final InventoryServiceContract inventoryService;

    public AdjustInventoryUseCase(
            ApiIdempotencyService idempotencyService, InventoryServiceContract inventoryService) {
        this.idempotencyService = idempotencyService;
        this.inventoryService = inventoryService;
    }

    public void execute(
            long userId, StockAdjustmentRequest request, Optional<String> idempotencyKeyHeader) {
        String fingerprint = RequestFingerprint.stockAdjustRequestHash(request);
        idempotencyService.runStockAdjust(
                userId,
                idempotencyKeyHeader,
                fingerprint,
                () -> inventoryService.applyManualStockAdjustment(userId, request));
    }
}
