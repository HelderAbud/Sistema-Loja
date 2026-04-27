package com.lojapp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.dto.brand.BrandRequest;
import com.lojapp.dto.dashboard.BrandDashboardResponse;
import com.lojapp.dto.dashboard.ProductAbcResponse;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class DashboardLoadIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("lojapp")
                    .withUsername("lojapp")
                    .withPassword("lojapp_test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("lojapp.jwt.secret", () -> "integration-test-secret-32-chars-min!!");
    }

    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LojappCatalogService catalog;
    @Autowired private InventoryService inventory;
    @Autowired private SalesService sales;
    @Autowired private DashboardService dashboard;

    @Test
    void dashboardWithHighVolume_returnsConsistentMetrics() {
        User u = new User();
        u.setEmail("dash-load-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);

        var alpha = catalog.createBrand(u.getId(), new BrandRequest("Alpha"));
        var beta = catalog.createBrand(u.getId(), new BrandRequest("Beta"));
        var pAlpha =
                catalog.createProduct(
                        u.getId(),
                        new ProductRequest(
                                "Prod Alpha",
                                alpha.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                BigDecimal.ZERO));
        var pBeta =
                catalog.createProduct(
                        u.getId(),
                        new ProductRequest(
                                "Prod Beta",
                                beta.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("5.00"),
                                new BigDecimal("12.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                u.getId(), new StockAdjustmentRequest(pAlpha.id(), new BigDecimal("5000"), "SEED"));
        inventory.adjustStock(
                u.getId(), new StockAdjustmentRequest(pBeta.id(), new BigDecimal("5000"), "SEED"));

        for (int i = 0; i < 1000; i++) {
            sales.registerSale(
                    u.getId(),
                    new SaleRequest(
                            pAlpha.id(), BigDecimal.ONE, new BigDecimal("20.00"), new BigDecimal("10.00")));
            sales.registerSale(
                    u.getId(),
                    new SaleRequest(
                            pBeta.id(), BigDecimal.ONE, new BigDecimal("12.00"), new BigDecimal("5.00")));
        }

        Instant from = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(2, ChronoUnit.MINUTES);
        long startNanos = System.nanoTime();
        BrandDashboardResponse brandDash = dashboard.brandDashboard(u.getId(), from, to, 50, 0);
        ProductAbcResponse abc = dashboard.productAbc(u.getId(), from, to);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertThat(brandDash.metrics()).hasSize(2);
        assertThat(brandDash.metrics().stream().map(m -> m.faturamento()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(new BigDecimal("32000.00"));
        assertThat(abc.rows()).hasSize(2);
        assertThat(abc.totalRevenue()).isEqualByComparingTo(new BigDecimal("32000.00"));
        // Guardrail de performance: as queries agregadas devem responder rápido mesmo com volume alto.
        assertThat(elapsedMs).isLessThan(5000);
    }

    @Test
    void productAbc_whenRevenueTies_keepsDeterministicOrderByProductId() {
        User u = new User();
        u.setEmail("dash-abc-tie-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);

        var brand = catalog.createBrand(u.getId(), new BrandRequest("Marca Tie"));
        var first =
                catalog.createProduct(
                        u.getId(),
                        new ProductRequest(
                                "Prod Tie A",
                                brand.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                BigDecimal.ZERO));
        var second =
                catalog.createProduct(
                        u.getId(),
                        new ProductRequest(
                                "Prod Tie B",
                                brand.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                u.getId(), new StockAdjustmentRequest(first.id(), new BigDecimal("100"), "SEED"));
        inventory.adjustStock(
                u.getId(), new StockAdjustmentRequest(second.id(), new BigDecimal("100"), "SEED"));

        sales.registerSale(
                u.getId(),
                new SaleRequest(first.id(), BigDecimal.ONE, new BigDecimal("20.00"), new BigDecimal("10.00")));
        sales.registerSale(
                u.getId(),
                new SaleRequest(second.id(), BigDecimal.ONE, new BigDecimal("20.00"), new BigDecimal("10.00")));

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(2, ChronoUnit.MINUTES);
        ProductAbcResponse abc = dashboard.productAbc(u.getId(), from, to);

        assertThat(abc.rows()).hasSize(2);
        assertThat(abc.rows().get(0).revenue()).isEqualByComparingTo(abc.rows().get(1).revenue());
        assertThat(abc.rows().get(0).productId()).isLessThan(abc.rows().get(1).productId());
    }
}
