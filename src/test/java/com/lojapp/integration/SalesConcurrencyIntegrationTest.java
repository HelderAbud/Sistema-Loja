package com.lojapp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.application.sale.CreatePosSaleUseCase;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.dto.sale.PosSaleFinalizeRequest;
import com.lojapp.dto.sale.PosSalePaymentRequest;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.entity.User;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.InventoryBalanceRepository;
import com.lojapp.repository.InventoryMovementRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.InventoryService;
import com.lojapp.service.LojappCatalogService;
import com.lojapp.service.SalesService;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class SalesConcurrencyIntegrationTest {

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
    @Autowired private InventoryBalanceRepository balances;
    @Autowired private SaleRepository saleRepository;
    @Autowired private SalePaymentRepository salePaymentRepository;
    @Autowired private InventoryMovementRepository movementRepository;
    @Autowired private CashSessionRepository cashSessions;
    @Autowired private CreatePosSaleUseCase createPosSaleUseCase;

    @Test
    void concurrentSales_doNotLetStockGoNegative() throws Exception {
        User u = new User();
        u.setEmail("sale-conc-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);
        final long userId = u.getId();

        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU Concorrencia",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("15.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "SEED"));
        Instant testStart = Instant.now();

        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> sellTask =
                () -> {
                    start.await();
                    try {
                        sales.registerSale(
                                userId,
                                new SaleRequest(
                                        product.id(),
                                        new BigDecimal("7"),
                                        new BigDecimal("15.00"),
                                        new BigDecimal("10.00")));
                        return true;
                    } catch (RuntimeException ex) {
                        return false;
                    }
                };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            futures.add(pool.submit(sellTask));
            futures.add(pool.submit(sellTask));
            start.countDown();

            int success = 0;
            for (Future<Boolean> f : futures) {
                if (f.get()) {
                    success++;
                }
            }

            BigDecimal finalQty =
                    balances
                            .findByUser_IdAndProduct_Id(userId, product.id())
                            .orElseThrow()
                            .getQuantity();

            assertThat(success).isEqualTo(1);
            assertThat(finalQty).isEqualByComparingTo(new BigDecimal("3"));

            var persistedSales =
                    saleRepository.findByUser_IdAndSoldAtBetween(
                            userId, testStart.minusSeconds(1), Instant.now().plusSeconds(1));
            assertThat(persistedSales).hasSize(1);
            assertThat(persistedSales.getFirst().getProduct().getId()).isEqualTo(product.id());
            assertThat(persistedSales.getFirst().getQuantity())
                    .isEqualByComparingTo(new BigDecimal("7"));

            long saleMovements =
                    movementRepository.findAll().stream()
                            .filter(m -> m.getUser().getId().equals(userId))
                            .filter(m -> m.getProduct().getId().equals(product.id()))
                            .filter(m -> "SALE".equals(m.getMovementType()))
                            .filter(m -> "SALE_REGISTER".equals(m.getSource()))
                            .count();
            assertThat(saleMovements).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentPosFinalize_doNotLetStockGoNegativeOrDuplicatePayments() throws Exception {
        User u = new User();
        u.setEmail("pos-conc-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);
        final long userId = u.getId();

        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU POS Concorrencia",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("15.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "SEED"));

        CashSession cashSession = new CashSession();
        cashSession.setUser(u);
        cashSession.setOpenedByUser(u);
        cashSession.setStatus(CashSessionStatus.OPEN);
        cashSession.setOpeningAmount(new BigDecimal("100.00"));
        cashSession = cashSessions.save(cashSession);
        final long cashSessionId = cashSession.getId();
        long paymentsBefore = salePaymentRepository.count();

        Instant testStart = Instant.now();

        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> finalizeTask =
                () -> {
                    start.await();
                    try {
                        createPosSaleUseCase.execute(
                                userId,
                                new PosSaleFinalizeRequest(
                                        cashSessionId,
                                        product.id(),
                                        new BigDecimal("7"),
                                        new BigDecimal("15.00"),
                                        new BigDecimal("10.00"),
                                        List.of(
                                                new PosSalePaymentRequest(
                                                        PaymentMethod.CARD, new BigDecimal("105.00")))),
                                Optional.of("pos-conc-" + UUID.randomUUID()));
                        return true;
                    } catch (RuntimeException ex) {
                        return false;
                    }
                };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            futures.add(pool.submit(finalizeTask));
            futures.add(pool.submit(finalizeTask));
            start.countDown();

            int success = 0;
            for (Future<Boolean> f : futures) {
                if (f.get()) {
                    success++;
                }
            }

            BigDecimal finalQty =
                    balances
                            .findByUser_IdAndProduct_Id(userId, product.id())
                            .orElseThrow()
                            .getQuantity();

            assertThat(success).isEqualTo(1);
            assertThat(finalQty).isEqualByComparingTo(new BigDecimal("3"));

            var persistedSales =
                    saleRepository.findByUser_IdAndSoldAtBetween(
                            userId, testStart.minusSeconds(1), Instant.now().plusSeconds(1));
            assertThat(persistedSales).hasSize(1);
            assertThat(persistedSales.getFirst().getCashSession()).isNotNull();
            assertThat(persistedSales.getFirst().getCashSession().getId()).isEqualTo(cashSessionId);

            assertThat(salePaymentRepository.count()).isEqualTo(paymentsBefore + 1);
            long paymentsForPersistedSale =
                    salePaymentRepository.findAll().stream()
                            .filter(p -> p.getSale().getId().equals(persistedSales.getFirst().getId()))
                            .count();
            assertThat(paymentsForPersistedSale).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
