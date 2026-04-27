package com.lojapp.dto.sale;

import com.lojapp.entity.Sale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Linha de venda no histórico")
public record SaleListItemResponse(
        @Schema(example = "101") Long id,
        @Schema(example = "12") Long productId,
        @Schema(example = "Caderno A4") String productName,
        @Schema(example = "Marca X") String brandName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal unitCost,
        Instant soldAt,
        @Schema(description = "True se a venda foi cancelada (fora dos totais do dashboard).")
                boolean cancelled) {

    public static SaleListItemResponse from(Sale sale) {
        var p = sale.getProduct();
        String brand =
                p.getBrand() == null ? "Nao informada" : p.getBrand().getName();
        return new SaleListItemResponse(
                sale.getId(),
                p.getId(),
                p.getName(),
                brand,
                sale.getQuantity(),
                sale.getUnitPrice(),
                sale.getUnitCost(),
                sale.getSoldAt(),
                sale.getCancelledAt() != null);
    }
}
