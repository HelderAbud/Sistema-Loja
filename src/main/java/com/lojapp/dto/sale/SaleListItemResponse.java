package com.lojapp.dto.sale;

import com.lojapp.entity.Sale;
import com.lojapp.entity.SaleItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
                boolean cancelled,
        @Schema(description = "Número de linhas em sale_items.") int itemCount,
        @Schema(description = "Soma qty × preço das linhas (não o header denormalizado).")
                BigDecimal lineTotal) {

    public static SaleListItemResponse from(Sale sale) {
        List<SaleItem> items = sale.getItems();
        if (items != null && !items.isEmpty()) {
            return fromItems(sale, items);
        }
        var p = sale.getProduct();
        String brand = p.getBrand() == null ? "Nao informada" : p.getBrand().getName();
        BigDecimal lineTotal = sale.getUnitPrice().multiply(sale.getQuantity());
        return new SaleListItemResponse(
                sale.getId(),
                p.getId(),
                p.getName(),
                brand,
                sale.getQuantity(),
                sale.getUnitPrice(),
                sale.getUnitCost(),
                sale.getSoldAt(),
                sale.getCancelledAt() != null,
                1,
                lineTotal);
    }

    private static SaleListItemResponse fromItems(Sale sale, List<SaleItem> items) {
        SaleItem first = items.get(0);
        var p = first.getProduct();
        String brand =
                p.getBrand() == null ? "Nao informada" : p.getBrand().getName();
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal lineTotal = BigDecimal.ZERO;
        for (SaleItem item : items) {
            quantity = quantity.add(item.getQuantity());
            lineTotal = lineTotal.add(item.getUnitPrice().multiply(item.getQuantity()));
        }
        String productName = p.getName();
        int extra = items.size() - 1;
        if (extra == 1) {
            productName = productName + " + 1 outro";
        } else if (extra > 1) {
            productName = productName + " + " + extra + " outros";
        }
        return new SaleListItemResponse(
                sale.getId(),
                p.getId(),
                productName,
                brand,
                quantity,
                first.getUnitPrice(),
                first.getUnitCost(),
                sale.getSoldAt(),
                sale.getCancelledAt() != null,
                items.size(),
                lineTotal);
    }
}
