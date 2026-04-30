package com.lojapp.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.brand.BrandRequest;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.BrandNotFoundException;
import com.lojapp.exception.domain.ProductNotFoundException;
import com.lojapp.service.InventoryService;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.LojappCatalogService;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class CatalogIsolationIntegrationTest {

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

    @Autowired private LojappCatalogService catalog;
    @Autowired private InventoryService inventory;
    @Autowired private UserRepository userRepository;

    private long userA;
    private long userB;

    @BeforeEach
    void setUp() {
        userA = createUser("catalog-a");
        userB = createUser("catalog-b");
    }

    @Test
    void listAndSearchProducts_returnOnlyOwnerData() {
        catalog.createProduct(
                userA,
                new ProductRequest(
                        "Produto A",
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("5.00"),
                        new BigDecimal("8.00"),
                        BigDecimal.ZERO));
        catalog.createProduct(
                userB,
                new ProductRequest(
                        "Produto B",
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("4.00"),
                        new BigDecimal("7.00"),
                        BigDecimal.ZERO));

        var userAList = catalog.listProducts(userA);
        var userBList = catalog.listProducts(userB);
        assertThat(userAList).extracting("name").containsExactly("Produto A");
        assertThat(userBList).extracting("name").containsExactly("Produto B");

        var userASearch = catalog.searchProducts(userA, null, "Produto", false, PageRequest.of(0, 20));
        assertThat(userASearch.getContent()).extracting("name").containsExactly("Produto A");
    }

    @Test
    void updateAndDeleteBrand_fromOtherUser_areBlockedByIsolation() {
        var brandA = catalog.createBrand(userA, new BrandRequest("Marca A"));
        catalog.createBrand(userB, new BrandRequest("Marca B"));

        assertThatThrownBy(() -> catalog.updateBrand(userB, brandA.id(), new BrandRequest("Invadir")))
                .isInstanceOf(BrandNotFoundException.class);

        assertThatThrownBy(() -> catalog.deleteBrand(userB, brandA.id()))
                .isInstanceOf(BrandNotFoundException.class);

        assertThat(catalog.listBrands(userA)).extracting("name").contains("Marca A");
        assertThat(catalog.listBrands(userB)).extracting("name").contains("Marca B");
    }

    @Test
    void updateProduct_fromOtherUser_throwsNotFound() {
        var productA =
                catalog.createProduct(
                        userA,
                        new ProductRequest(
                                "Produto Dono A",
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("3.00"),
                                new BigDecimal("6.00"),
                                BigDecimal.ZERO));

        assertThatThrownBy(
                        () ->
                                catalog.updateProduct(
                                        userB,
                                        productA.id(),
                                        new ProductRequest(
                                                "Tentativa Cross User",
                                                null,
                                                null,
                                                null,
                                                null,
                                                new BigDecimal("3.00"),
                                                new BigDecimal("6.00"),
                                                BigDecimal.ZERO)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void searchProducts_filtersByBrandNameLowStockAndPagination() {
        var brandA = catalog.createBrand(userA, new BrandRequest("Marca A"));
        var brandB = catalog.createBrand(userA, new BrandRequest("Marca B"));

        var alphaA =
                catalog.createProduct(
                        userA,
                        new ProductRequest(
                                "Alpha Caderno",
                                brandA.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("5.00"),
                                new BigDecimal("8.00"),
                                new BigDecimal("5")));
        var alphaB =
                catalog.createProduct(
                        userA,
                        new ProductRequest(
                                "Alpha Estojo",
                                brandB.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("4.00"),
                                new BigDecimal("7.00"),
                                new BigDecimal("1")));
        var betaA =
                catalog.createProduct(
                        userA,
                        new ProductRequest(
                                "Beta Agenda",
                                brandA.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("6.00"),
                                new BigDecimal("9.00"),
                                new BigDecimal("2")));

        inventory.adjustStock(
                userA, new StockAdjustmentRequest(alphaA.id(), new BigDecimal("2"), "SEED LOW"));
        inventory.adjustStock(
                userA, new StockAdjustmentRequest(alphaB.id(), new BigDecimal("2"), "SEED OK"));
        inventory.adjustStock(
                userA, new StockAdjustmentRequest(betaA.id(), new BigDecimal("1"), "SEED LOW"));

        var byBrandA =
                catalog.searchProducts(
                        userA,
                        brandA.id(),
                        null,
                        false,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name")));
        assertThat(byBrandA.getContent()).extracting("name").containsExactly("Alpha Caderno", "Beta Agenda");

        var byQuery =
                catalog.searchProducts(
                        userA,
                        null,
                        "alpha",
                        false,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name")));
        assertThat(byQuery.getContent()).extracting("name").containsExactly("Alpha Caderno", "Alpha Estojo");

        var lowStockOnly =
                catalog.searchProducts(
                        userA,
                        null,
                        null,
                        true,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name")));
        assertThat(lowStockOnly.getContent()).extracting("name").containsExactly("Alpha Caderno", "Beta Agenda");

        var page0 =
                catalog.searchProducts(
                        userA,
                        null,
                        null,
                        false,
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "name")));
        var page1 =
                catalog.searchProducts(
                        userA,
                        null,
                        null,
                        false,
                        PageRequest.of(1, 1, Sort.by(Sort.Direction.ASC, "name")));

        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page0.getTotalPages()).isEqualTo(3);
        assertThat(page0.getContent()).extracting("name").containsExactly("Alpha Caderno");
        assertThat(page1.getContent()).extracting("name").containsExactly("Alpha Estojo");
    }

    private long createUser(String tag) {
        User user = new User();
        user.setEmail(tag + "-" + Instant.now().toEpochMilli() + "@test.local");
        user.setPasswordHash("noop");
        user.setAppRole("LOJA_USER");
        return userRepository.save(user).getId();
    }
}
