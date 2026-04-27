package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.entity.User;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.brand.BrandRequest;
import com.lojapp.dto.hierarchy.ProductCollectionRequest;
import com.lojapp.dto.hierarchy.ProductModelRequest;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.dto.supplier.SupplierRequest;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.SupplierRepository;
import com.lojapp.repository.UserRepository;
import java.util.Optional;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LojappHierarchyServiceTest {

    @Autowired private UserRepository userRepository;
    @Autowired private LojappCatalogService catalog;
    @Autowired private LojappHierarchyService hierarchy;
    @Autowired private SupplierRepository supplierRepository;

    private long userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("hier-test-" + System.nanoTime() + "@test.local");
        user.setPasswordHash("noop");
        user.setAppRole("LOJA_USER");
        userId = userRepository.save(user).getId();
    }

    @Test
    void resolveSupplierForNfeImport_twiceSameTaxId_reusesRow() {
        var s1 =
                hierarchy.resolveSupplierForNfeImport(
                        userId, Optional.of("99888777000166"), "Emitente Um");
        var s2 =
                hierarchy.resolveSupplierForNfeImport(
                        userId, Optional.of("99888777000166"), "Emitente Dois");
        assertThat(s1.getId()).isEqualTo(s2.getId());
        assertThat(supplierRepository.findByUser_IdOrderByLegalNameAsc(userId)).hasSize(1);
    }

    @Test
    void resolveSupplierForNfeImport_noTaxId_returnsNull() {
        assertThat(hierarchy.resolveSupplierForNfeImport(userId, Optional.empty(), "Só Nome"))
                .isNull();
    }

    @Test
    void createSupplier_duplicateTaxId_conflict() {
        hierarchy.createSupplier(userId, new SupplierRequest("A", "11.222.333/0001-81"));
        assertThatThrownBy(
                        () ->
                                hierarchy.createSupplier(
                                        userId, new SupplierRequest("B", "11222333000181")))
                .isInstanceOfSatisfying(
                        LojappDomainException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.CONFLICT));
    }

    @Test
    void createCollection_duplicateNameCaseInsensitive_conflict() {
        long brandId = catalog.createBrand(userId, new BrandRequest("Marca H")).id();
        hierarchy.createCollection(userId, new ProductCollectionRequest(brandId, "Verão"));
        assertThatThrownBy(
                        () ->
                                hierarchy.createCollection(
                                        userId, new ProductCollectionRequest(brandId, "VERÃO")))
                .isInstanceOfSatisfying(
                        LojappDomainException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.CONFLICT));
    }

    @Test
    void createModel_withCollection_linksProductViaCatalog() {
        long brandId = catalog.createBrand(userId, new BrandRequest("Marca M")).id();
        long colId =
                hierarchy
                        .createCollection(userId, new ProductCollectionRequest(brandId, "Col"))
                        .id();
        long modelId =
                hierarchy
                        .createModel(
                                userId,
                                new ProductModelRequest(brandId, colId, "Camisa polo"))
                        .id();

        var product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Camisa polo M Azul",
                                brandId,
                                null,
                                null,
                                null,
                                BigDecimal.TEN,
                                BigDecimal.valueOf(29),
                                BigDecimal.ZERO,
                                null,
                                modelId,
                                "Azul",
                                "M"));

        assertThat(product.productModelId()).isEqualTo(modelId);
        assertThat(product.variantColor()).isEqualTo("Azul");
        assertThat(product.variantSize()).isEqualTo("M");
    }
}
