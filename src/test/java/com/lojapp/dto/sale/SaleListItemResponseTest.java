package com.lojapp.dto.sale;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.entity.Product;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SaleItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SaleListItemResponseTest {

    @Test
    void from_multiItemSale_aggregatesQuantityAndLineTotal() {
        Product caderno = new Product();
        caderno.setId(1L);
        caderno.setName("Caderno A4");
        Product caneta = new Product();
        caneta.setId(2L);
        caneta.setName("Caneta");

        Sale sale = new Sale();
        sale.setId(50L);
        sale.setProduct(caderno);
        sale.setQuantity(new BigDecimal("1"));
        sale.setUnitPrice(new BigDecimal("10.00"));
        sale.setUnitCost(new BigDecimal("4.00"));
        sale.setSoldAt(Instant.parse("2026-09-08T12:00:00Z"));

        SaleItem line1 = new SaleItem();
        line1.setProduct(caderno);
        line1.setQuantity(new BigDecimal("1"));
        line1.setUnitPrice(new BigDecimal("10.00"));
        line1.setUnitCost(new BigDecimal("4.00"));
        SaleItem line2 = new SaleItem();
        line2.setProduct(caneta);
        line2.setQuantity(new BigDecimal("2"));
        line2.setUnitPrice(new BigDecimal("5.00"));
        line2.setUnitCost(new BigDecimal("1.00"));
        sale.setItems(List.of(line1, line2));

        SaleListItemResponse row = SaleListItemResponse.from(sale);

        assertThat(row.itemCount()).isEqualTo(2);
        assertThat(row.quantity()).isEqualByComparingTo("3");
        assertThat(row.lineTotal()).isEqualByComparingTo("20.00");
        assertThat(row.productName()).isEqualTo("Caderno A4 + 1 outro");
        assertThat(row.productId()).isEqualTo(1L);
    }
}
