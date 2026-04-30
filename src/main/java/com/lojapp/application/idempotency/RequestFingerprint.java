package com.lojapp.application.idempotency;

import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.dto.sale.PosSaleFinalizeRequest;
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

    public static String posSaleFinalizeRequestHash(PosSaleFinalizeRequest r) {
        String unitCost = r.unitCost() == null ? "" : r.unitCost().stripTrailingZeros().toPlainString();
        String payments =
                r.payments().stream()
                        .map(
                                p ->
                                        p.paymentMethod().name()
                                                + ":"
                                                + p.amount().stripTrailingZeros().toPlainString())
                        .sorted()
                        .reduce((a, b) -> a + "|" + b)
                        .orElse("");
        String raw =
                r.cashSessionId()
                        + "|"
                        + r.productId()
                        + "|"
                        + r.quantity().stripTrailingZeros().toPlainString()
                        + "|"
                        + r.unitPrice().stripTrailingZeros().toPlainString()
                        + "|"
                        + unitCost
                        + "|"
                        + payments;
        return TokenHashUtil.sha256Hex(raw);
    }
}
