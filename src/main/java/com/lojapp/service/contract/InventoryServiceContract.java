package com.lojapp.service.contract;

import com.lojapp.dto.dashboard.InventoryKpiResponse;
import com.lojapp.dto.inventory.LowStockResponse;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.entity.Product;
import com.lojapp.entity.User;
import java.math.BigDecimal;
import java.util.List;

public interface InventoryServiceContract {

    void decreaseForSale(User user, Product product, BigDecimal quantitySold, long saleId);

    /** Repõe stock após cancelamento de venda (movimento de ajuste positivo ligado ao saleId). */
    void restoreStockForCancelledSale(User user, Product product, BigDecimal quantitySold, long saleId);

    void increaseFromNfe(User user, Product product, BigDecimal quantity, long nfeEntryId);

    BigDecimal getAvailableQuantity(long userId, long productId);

    BigDecimal getStockForOwnedProduct(long userId, long productId);

    void assertSufficientStock(long userId, long productId, BigDecimal quantityRequested);

    default void adjustStock(long userId, StockAdjustmentRequest request) {
        applyManualStockAdjustment(userId, request);
    }

    /**
     * Aplica movimento de ajuste (delta + motivo). Para API HTTP usar
     * AdjustInventoryUseCaseContract com idempotência; este método expõe o núcleo transacional.
     */
    void applyManualStockAdjustment(long userId, StockAdjustmentRequest request);

    InventoryKpiResponse inventoryKpis(long userId);

    List<LowStockResponse> listLowStock(long userId);
}
