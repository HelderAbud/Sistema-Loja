package com.lojapp.application.idempotency;

import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.util.TokenHashUtil;

public final class RequestFingerprint {

    private RequestFingerprint() {}

    public static String saleRequestHash(SaleRequest r) {
        String uc = r.unitCost() == null ? "" : r.unitCost().stripTrailingZeros().toPlainString();
        String raw =
                r.productId()
                        + "|"
                        + r.quantity().stripTrailingZeros().toPlainString()
                        + "|"
                        + r.unitPrice().stripTrailingZeros().toPlainString()
                        + "|"
                        + uc;
        return TokenHashUtil.sha256Hex(raw);
    }

    public static String stockAdjustRequestHash(StockAdjustmentRequest r) {
        String reason = r.reason() == null ? "" : r.reason().trim();
        String raw =
                r.productId()
                        + "|"
                        + r.quantity().stripTrailingZeros().toPlainString()
                        + "|"
                        + reason;
        return TokenHashUtil.sha256Hex(raw);
    }
}
