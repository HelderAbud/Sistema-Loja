package com.lojapp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.application.nfe.ImportNfeUseCase;
import com.lojapp.dto.brand.BrandRequest;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.entity.User;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.DashboardService;
import com.lojapp.service.InventoryService;
import com.lojapp.service.LojappCatalogService;
import com.lojapp.service.SalesService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class PerformanceP2SmokeIntegrationTest {

    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LojappCatalogService catalog;
    @Autowired private InventoryService inventory;
    @Autowired private SalesService sales;
    @Autowired private DashboardService dashboard;
    @Autowired private ImportNfeUseCase importNfeUseCase;

    @Test
    void benchmarkCriticalFlows_reportsP95AndP99() {
        User user = new User();
        user.setEmail("perf-smoke-" + UUID.randomUUID() + "@test.com");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user = users.save(user);

        var brand = catalog.createBrand(user.getId(), new BrandRequest("Perf Brand"));
        var product =
                catalog.createProduct(
                        user.getId(),
                        new ProductRequest(
                                "Perf Product",
                                brand.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                user.getId(), new StockAdjustmentRequest(product.id(), new BigDecimal("5000"), "SEED"));
        for (int i = 0; i < 400; i++) {
            sales.registerSale(
                    user.getId(),
                    new SaleRequest(
                            product.id(),
                            BigDecimal.ONE,
                            new BigDecimal("20.00"),
                            new BigDecimal("10.00")));
        }

        final long userId = user.getId();
        Instant from = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(2, ChronoUnit.MINUTES);

        MetricsSummary salesSummary = measure(60, () -> sales.summarizeSales(userId, from, to, null, null));
        MetricsSummary dashboardBrands = measure(60, () -> dashboard.brandDashboard(userId, from, to, 50, 0));
        MetricsSummary nfeImport = measure(25, new NfeImportRunnable(userId));

        System.out.printf(
                "PERF_SMOKE sales.summary p95=%dms p99=%dms avg=%dms%n",
                salesSummary.p95Ms, salesSummary.p99Ms, salesSummary.avgMs);
        System.out.printf(
                "PERF_SMOKE dashboard.brands p95=%dms p99=%dms avg=%dms%n",
                dashboardBrands.p95Ms, dashboardBrands.p99Ms, dashboardBrands.avgMs);
        System.out.printf(
                "PERF_SMOKE nfe.import p95=%dms p99=%dms avg=%dms%n",
                nfeImport.p95Ms, nfeImport.p99Ms, nfeImport.avgMs);

        assertThat(salesSummary.p95Ms).isGreaterThanOrEqualTo(0);
        assertThat(dashboardBrands.p95Ms).isGreaterThanOrEqualTo(0);
        assertThat(nfeImport.p95Ms).isGreaterThanOrEqualTo(0);
    }

    private static MetricsSummary measure(int iterations, Runnable runnable) {
        List<Long> elapsed = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            runnable.run();
            long ms = (System.nanoTime() - start) / 1_000_000;
            elapsed.add(ms);
        }
        elapsed.sort(Long::compareTo);
        long p95 = percentile(elapsed, 95);
        long p99 = percentile(elapsed, 99);
        long avg =
                Math.round(elapsed.stream().mapToLong(Long::longValue).average().orElse(0.0d));
        return new MetricsSummary(p95, p99, avg);
    }

    private static long percentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0L;
        int index = (int) Math.ceil((percentile / 100.0d) * values.size()) - 1;
        int safe = Math.max(0, Math.min(values.size() - 1, index));
        return values.get(safe);
    }

    private record MetricsSummary(long p95Ms, long p99Ms, long avgMs) {}

    private final class NfeImportRunnable implements Runnable {
        private final long userId;
        private int sequence = 0;

        private NfeImportRunnable(long userId) {
            this.userId = userId;
        }

        @Override
        public void run() {
            sequence++;
            String key = String.format("352001111111111111115500100000100110000%06d", sequence);
            String xml =
                    """
                    <nfe>
                      <nNF>%d</nNF>
                      <xNome>Fornecedor Perf</xNome>
                      <chNFe>%s</chNFe>
                      <prod>
                        <xProd>Perf XML Item</xProd>
                        <qCom>1</qCom>
                        <vUnCom>10.00</vUnCom>
                      </prod>
                    </nfe>
                    """
                            .formatted(10_000 + sequence, key);
            importNfeUseCase.execute(userId, xml);
        }
    }
}
