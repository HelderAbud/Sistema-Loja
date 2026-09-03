package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lojapp.dto.commission.CommissionAccrualResponse;
import com.lojapp.entity.Brand;
import com.lojapp.entity.CommissionAccrual;
import com.lojapp.entity.Sale;
import com.lojapp.entity.Seller;
import com.lojapp.repository.CommissionAccrualRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommissionReportServiceTest {

    @Mock private CommissionAccrualRepository accruals;

    private CommissionReportService service;

    @BeforeEach
    void setUp() {
        service = new CommissionReportService(accruals);
    }

    @Test
    void list_mapsSellerBrandAndAmounts() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");
        when(accruals.findByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(1L, from, to))
                .thenReturn(List.of(accrual()));

        List<CommissionAccrualResponse> rows = service.list(1L, from, to);

        assertThat(rows).hasSize(1);
        CommissionAccrualResponse row = rows.get(0);
        assertThat(row.id()).isEqualTo(5L);
        assertThat(row.saleId()).isEqualTo(88L);
        assertThat(row.sellerId()).isEqualTo(11L);
        assertThat(row.sellerName()).isEqualTo("Ana, loja");
        assertThat(row.brandId()).isEqualTo(8L);
        assertThat(row.brandName()).isEqualTo("MarcaX");
        assertThat(row.amount()).isEqualByComparingTo("2.40");
    }

    @Test
    void toCsv_escapesCommaInSellerName() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");
        when(accruals.findByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(1L, from, to))
                .thenReturn(List.of(accrual()));

        String csv = service.toCsv(1L, from, to);

        assertThat(csv).startsWith("id,saleId,sellerId,sellerName,brandId,brandName,baseAmount,percent,amount,createdAt");
        assertThat(csv).contains("\"Ana, loja\"");
        assertThat(csv).contains("2.40");
    }

    @Test
    void toCsv_prefixesFormulaTriggerInSellerName() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");
        when(accruals.findByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(1L, from, to))
                .thenReturn(List.of(accrual("=1+1", "MarcaX")));

        String csv = service.toCsv(1L, from, to);

        assertThat(csv).contains(",'=1+1,");
        assertThat(csv).doesNotContain(",=1+1,");
    }

    @Test
    void toCsv_prefixesFormulaTriggerAndQuotesWhenNameHasComma() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");
        when(accruals.findByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(1L, from, to))
                .thenReturn(List.of(accrual("=HYPERLINK(\"http://evil.test\",\"x\")", "MarcaX")));

        String csv = service.toCsv(1L, from, to);

        assertThat(csv)
                .contains("\"'=HYPERLINK(\"\"http://evil.test\"\",\"\"x\"\")\"");
    }

    private static CommissionAccrual accrual() {
        return accrual("Ana, loja", "MarcaX");
    }

    private static CommissionAccrual accrual(String sellerName, String brandName) {
        Sale sale = new Sale();
        sale.setId(88L);
        Seller seller = new Seller();
        seller.setId(11L);
        seller.setDisplayName(sellerName);
        Brand brand = new Brand();
        brand.setId(8L);
        brand.setName(brandName);
        CommissionAccrual row = new CommissionAccrual();
        row.setId(5L);
        row.setSale(sale);
        row.setSeller(seller);
        row.setBrand(brand);
        row.setBaseAmount(new BigDecimal("20.00"));
        row.setPercent(new BigDecimal("12.0000"));
        row.setAmount(new BigDecimal("2.40"));
        row.setCreatedAt(Instant.parse("2026-08-15T12:00:00Z"));
        return row;
    }
}
