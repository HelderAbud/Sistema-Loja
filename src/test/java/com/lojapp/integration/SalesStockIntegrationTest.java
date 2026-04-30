package com.lojapp.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.dto.sale.PosSaleFinalizeRequest;
import com.lojapp.dto.sale.PosSalePaymentRequest;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.InventoryMovement;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.CashSessionNotOpenException;
import com.lojapp.exception.domain.InsufficientStockException;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.PosSalePaymentTotalMismatchException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.ApiIdempotencyRepository;
import com.lojapp.repository.InventoryBalanceRepository;
import com.lojapp.repository.InventoryMovementRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.application.sale.CreatePosSaleUseCase;
import com.lojapp.service.InventoryService;
import com.lojapp.service.LojappCatalogService;
import com.lojapp.service.SalesService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
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
class SalesStockIntegrationTest {

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
    @Autowired private SalesService salesService;
    @Autowired private SaleRepository sales;
    @Autowired private InventoryBalanceRepository balances;
    @Autowired private InventoryMovementRepository movements;
    @Autowired private ApiIdempotencyRepository apiIdempotency;
    @Autowired private CashSessionRepository cashSessions;
    @Autowired private SalePaymentRepository salePayments;
    @Autowired private CreatePosSaleUseCase createPosSaleUseCase;

    @Test
    void registerSale_decreasesStockAndPersistsSaleMovement() {
        User user = createUser("sale-stock-ok");
        long userId = user.getId();

        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU Integracao Venda",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("15.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("8"), "SEED"));

        var created =
                salesService.registerSale(
                        userId,
                        new SaleRequest(
                                product.id(),
                                new BigDecimal("3"),
                                new BigDecimal("15.00"),
                                new BigDecimal("10.00")));

        BigDecimal finalQty =
                balances.findByUser_IdAndProduct_Id(userId, product.id()).orElseThrow().getQuantity();
        assertThat(finalQty).isEqualByComparingTo(new BigDecimal("5"));

        Instant from = Instant.now().minusSeconds(60);
        Instant to = Instant.now().plusSeconds(60);
        var persistedSales = sales.findByUser_IdAndSoldAtBetween(userId, from, to);
        assertThat(persistedSales).extracting("id").contains(created.id());

        InventoryMovement saleMovement =
                movements.findAll().stream()
                        .filter(m -> m.getUser().getId().equals(userId))
                        .filter(m -> m.getProduct().getId().equals(product.id()))
                        .filter(m -> "SALE".equals(m.getMovementType()))
                        .filter(m -> "SALE_REGISTER".equals(m.getSource()))
                        .filter(m -> m.getSourceId() != null && m.getSourceId() == created.id())
                        .findFirst()
                        .orElseThrow();

        assertThat(saleMovement.getQuantity()).isEqualByComparingTo(new BigDecimal("-3"));
    }

    @Test
    void registerSale_whenInsufficientStock_rollsBackSaleAndMovement() {
        User user = createUser("sale-stock-fail");
        long userId = user.getId();

        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU Integracao Sem Saldo",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("2"), "SEED"));

        assertThatThrownBy(
                        () ->
                                salesService.registerSale(
                                        userId,
                                        new SaleRequest(
                                                product.id(),
                                                new BigDecimal("3"),
                                                new BigDecimal("20.00"),
                                                new BigDecimal("10.00"))))
                .isInstanceOf(InsufficientStockException.class);

        BigDecimal finalQty =
                balances.findByUser_IdAndProduct_Id(userId, product.id()).orElseThrow().getQuantity();
        assertThat(finalQty).isEqualByComparingTo(new BigDecimal("2"));

        Instant from = Instant.now().minusSeconds(60);
        Instant to = Instant.now().plusSeconds(60);
        assertThat(sales.findByUser_IdAndSoldAtBetween(userId, from, to)).isEmpty();

        boolean hasSaleMovement =
                movements.findAll().stream()
                        .anyMatch(
                                m ->
                                        m.getUser().getId().equals(userId)
                                                && m.getProduct().getId().equals(product.id())
                                                && "SALE".equals(m.getMovementType())
                                                && "SALE_REGISTER".equals(m.getSource()));
        assertThat(hasSaleMovement).isFalse();
    }

    @Test
    void registerSale_sameIdempotencyKeyAndBody_singleSale() {
        User user = createUser("sale-idem");
        long userId = user.getId();
        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU Idem Venda",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("15.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "SEED"));

        long salesCountBefore = sales.count();
        String key = "idem-sale-" + UUID.randomUUID();
        var req =
                new SaleRequest(
                        product.id(),
                        new BigDecimal("2"),
                        new BigDecimal("12.00"),
                        new BigDecimal("10.00"));
        var first = salesService.registerSale(userId, req, Optional.of(key));
        var second = salesService.registerSale(userId, req, Optional.of(key));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(sales.count()).isEqualTo(salesCountBefore + 1);
        assertThat(apiIdempotency.countByUser_Id(userId)).isEqualTo(1);
    }

    @Test
    void registerSale_sameIdempotencyKeyDifferentBody_conflict() {
        User user = createUser("sale-idem-conflict");
        long userId = user.getId();
        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU Idem Conflict",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("15.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "SEED"));

        String key = "idem-conflict-" + UUID.randomUUID();
        salesService.registerSale(
                userId,
                new SaleRequest(
                        product.id(),
                        new BigDecimal("1"),
                        new BigDecimal("12.00"),
                        new BigDecimal("10.00")),
                Optional.of(key));

        assertThatThrownBy(
                        () ->
                                salesService.registerSale(
                                        userId,
                                        new SaleRequest(
                                                product.id(),
                                                new BigDecimal("2"),
                                                new BigDecimal("12.00"),
                                                new BigDecimal("10.00")),
                                        Optional.of(key)))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.CONFLICT));
    }

    @Test
    void adjustStock_sameIdempotencyKey_singleMovement() {
        User user = createUser("adjust-idem");
        long userId = user.getId();
        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU Idem Ajuste",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("1.00"),
                                new BigDecimal("2.00"),
                                BigDecimal.ZERO));

        long movementsBefore = movements.count();
        String key = "idem-adj-" + UUID.randomUUID();
        var req = new StockAdjustmentRequest(product.id(), new BigDecimal("3"), "INV");
        inventory.adjustStock(userId, req, Optional.of(key));
        inventory.adjustStock(userId, req, Optional.of(key));

        assertThat(movements.count()).isEqualTo(movementsBefore + 1);
    }

    @Test
    void cancelSale_restoresStock() {
        User user = createUser("sale-cancel");
        long userId = user.getId();

        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU Cancel Test",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "SEED"));

        var created =
                salesService.registerSale(
                        userId,
                        new SaleRequest(
                                product.id(),
                                new BigDecimal("4"),
                                new BigDecimal("20.00"),
                                new BigDecimal("10.00")));

        assertThat(
                        balances.findByUser_IdAndProduct_Id(userId, product.id()).orElseThrow().getQuantity())
                .isEqualByComparingTo(new BigDecimal("6"));

        salesService.cancelSale(userId, created.id());

        assertThat(
                        balances.findByUser_IdAndProduct_Id(userId, product.id()).orElseThrow().getQuantity())
                .isEqualByComparingTo(new BigDecimal("10"));
        assertThat(sales.findById(created.id()).orElseThrow().getCancelledAt()).isNotNull();

        boolean hasCancelMovement =
                movements.findAll().stream()
                        .anyMatch(
                                m ->
                                        m.getUser().getId().equals(userId)
                                                && m.getProduct().getId().equals(product.id())
                                                && "SALE_CANCEL".equals(m.getSource())
                                                && m.getSourceId() != null
                                                && m.getSourceId() == created.id());
        assertThat(hasCancelMovement).isTrue();
    }

    @Test
    void finalizePosSale_whenNoOpenCashSession_throwsConflict() {
        User user = createUser("pos-no-open-session");
        long userId = user.getId();

        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU POS NO SESSION",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "SEED"));

        CashSession closedSession = createCashSession(user, CashSessionStatus.CLOSED, new BigDecimal("100.00"));

        assertThatThrownBy(
                        () ->
                                createPosSaleUseCase.execute(
                                        userId,
                                        new PosSaleFinalizeRequest(
                                                closedSession.getId(),
                                                product.id(),
                                                new BigDecimal("1"),
                                                new BigDecimal("20.00"),
                                                new BigDecimal("10.00"),
                                                java.util.List.of(
                                                        new PosSalePaymentRequest(
                                                                PaymentMethod.CASH,
                                                                new BigDecimal("20.00")))),
                                        Optional.of("pos-no-open-" + UUID.randomUUID())))
                .isInstanceOf(CashSessionNotOpenException.class);
    }

    @Test
    void finalizePosSale_whenPaymentTotalMismatch_throwsBadRequest() {
        User user = createUser("pos-payment-mismatch");
        long userId = user.getId();

        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU POS MISMATCH",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("25.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "SEED"));

        CashSession openSession = createCashSession(user, CashSessionStatus.OPEN, new BigDecimal("200.00"));

        assertThatThrownBy(
                        () ->
                                createPosSaleUseCase.execute(
                                        userId,
                                        new PosSaleFinalizeRequest(
                                                openSession.getId(),
                                                product.id(),
                                                new BigDecimal("2"),
                                                new BigDecimal("25.00"),
                                                new BigDecimal("10.00"),
                                                java.util.List.of(
                                                        new PosSalePaymentRequest(
                                                                PaymentMethod.CARD,
                                                                new BigDecimal("30.00")))),
                                        Optional.of("pos-mismatch-" + UUID.randomUUID())))
                .isInstanceOf(PosSalePaymentTotalMismatchException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void finalizePosSale_sameIdempotencyKey_singleSaleAndPayments() {
        User user = createUser("pos-idem");
        long userId = user.getId();

        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU POS IDEM",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "SEED"));
        CashSession openSession = createCashSession(user, CashSessionStatus.OPEN, new BigDecimal("100.00"));

        long salesBefore = sales.count();
        long paymentsBefore = salePayments.count();
        String key = "pos-idem-" + UUID.randomUUID();
        PosSaleFinalizeRequest req =
                new PosSaleFinalizeRequest(
                        openSession.getId(),
                        product.id(),
                        new BigDecimal("2"),
                        new BigDecimal("20.00"),
                        new BigDecimal("10.00"),
                        java.util.List.of(
                                new PosSalePaymentRequest(PaymentMethod.CASH, new BigDecimal("10.00")),
                                new PosSalePaymentRequest(PaymentMethod.PIX, new BigDecimal("30.00"))));

        var first = createPosSaleUseCase.execute(userId, req, Optional.of(key));
        var second = createPosSaleUseCase.execute(userId, req, Optional.of(key));

        assertThat(second.saleId()).isEqualTo(first.saleId());
        assertThat(sales.count()).isEqualTo(salesBefore + 1);
        assertThat(salePayments.count()).isEqualTo(paymentsBefore + 2);
    }

    private User createUser(String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@test.com");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        return users.save(user);
    }

    private CashSession createCashSession(User user, CashSessionStatus status, BigDecimal openingAmount) {
        CashSession session = new CashSession();
        session.setUser(user);
        session.setOpenedByUser(user);
        session.setStatus(status);
        session.setOpeningAmount(openingAmount);
        if (status == CashSessionStatus.CLOSED) {
            session.setClosedByUser(user);
            session.setClosedAt(Instant.now());
        }
        return cashSessions.save(session);
    }
}
