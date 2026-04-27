package com.lojapp.domain.inventory;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.exception.domain.LojappDomainException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Comando de domínio: ajuste manual de stock (delta + motivo normalizado).
 *
 * <p>Reforça invariantes também quando o pedido não passa pela validação Bean Validation (ex. chamadas
 * internas a {@link com.lojapp.service.InventoryService#applyManualStockAdjustment}).
 */
public record ManualStockAdjustment(long productId, BigDecimal quantity, String reason) {

    private static final int REASON_MAX_LEN = 500;

    public ManualStockAdjustment {
        if (productId <= 0) {
            throw new LojappDomainException(ApiErrorCode.BAD_REQUEST, "productId inválido");
        }
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new LojappDomainException(ApiErrorCode.BAD_REQUEST, "quantity não pode ser zero");
        }
        Objects.requireNonNull(reason, "reason");
        String normalized = reason.trim();
        if (normalized.isEmpty()) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST,
                    "Motivo do ajuste é obrigatório (após remover espaços).");
        }
        if (normalized.length() > REASON_MAX_LEN) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST, "Motivo do ajuste excede 500 caracteres.");
        }
        reason = normalized;
    }

    public static ManualStockAdjustment fromRequest(StockAdjustmentRequest request) {
        Objects.requireNonNull(request, "request");
        Long pid = request.productId();
        if (pid == null) {
            throw new LojappDomainException(ApiErrorCode.BAD_REQUEST, "productId inválido");
        }
        String rawReason = request.reason() == null ? "" : request.reason();
        return new ManualStockAdjustment(pid, request.quantity(), rawReason);
    }
}
