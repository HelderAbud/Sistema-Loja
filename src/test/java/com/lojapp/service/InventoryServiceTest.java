package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lojapp.entity.InventoryBalance;
import com.lojapp.entity.InventoryMovement;
import com.lojapp.entity.Product;
import com.lojapp.entity.User;
import com.lojapp.repository.InventoryBalanceRepository;
import com.lojapp.repository.InventoryMovementRepository;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.exception.domain.InsufficientStockException;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.ProductNotFoundException;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private ProductRepository products;
    @Mock private UserRepository users;
    @Mock private InventoryMovementRepository inventoryMovements;
    @Mock private InventoryBalanceRepository inventoryBalances;

    @Mock private AuditService auditService;

    @InjectMocks private InventoryService inventoryService;

    @Test
    void decreaseForSale_rejectsNonPositiveQuantity() {
        User user = new User();
        Product product = new Product();

        assertThatThrownBy(
                        () ->
                                inventoryService.decreaseForSale(
                                        user, product, BigDecimal.ZERO, 1L))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));

        verifyNoInteractions(inventoryMovements, inventoryBalances);
    }

    @Test
    void increaseFromNfe_rejectsNonPositiveQuantity() {
        assertThatThrownBy(
                        () ->
                                inventoryService.increaseFromNfe(
                                        new User(), new Product(), new BigDecimal("-1"), 1L))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));

        verifyNoInteractions(inventoryMovements, inventoryBalances);
    }

    @Test
    void getStockForOwnedProduct_throwsWhenProductMissing() {
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getStockForOwnedProduct(1L, 9L))
                .isInstanceOf(ProductNotFoundException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.NOT_FOUND));

        verifyNoInteractions(inventoryBalances);
    }

    @Test
    void getStockForOwnedProduct_returnsZeroWhenNoBalanceRow() {
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(9L, 1L)).thenReturn(Optional.of(new Product()));
        when(inventoryBalances.findByUser_IdAndProduct_Id(1L, 9L)).thenReturn(Optional.empty());

        assertThat(inventoryService.getStockForOwnedProduct(1L, 9L)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getStockForOwnedProduct_returnsBalance() {
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(9L, 1L)).thenReturn(Optional.of(new Product()));
        InventoryBalance b = new InventoryBalance();
        b.setQuantity(new BigDecimal("3.5"));
        when(inventoryBalances.findByUser_IdAndProduct_Id(1L, 9L)).thenReturn(Optional.of(b));

        assertThat(inventoryService.getStockForOwnedProduct(1L, 9L)).isEqualByComparingTo("3.5");
    }

    @Test
    void adjustStock_blankReason_rejectsWithBadRequest() {
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(10L, new BigDecimal("2"), "   ");

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, request))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));

        verifyNoInteractions(products, users, inventoryMovements, inventoryBalances, auditService);
    }

    @Test
    void adjustStock_productNotOwned_throwsNotFound() {
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(55L, 1L)).thenReturn(Optional.empty());
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(55L, new BigDecimal("2"), "Ajuste manual");

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, request))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(users, inventoryMovements, inventoryBalances, auditService);
    }

    @Test
    void adjustStock_aliasAppliesManualAdjustmentWithoutIdempotency() {
        Product owned = new Product();
        owned.setId(55L);
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(55L, 1L)).thenReturn(Optional.of(owned));
        User owner = new User();
        owner.setId(1L);
        when(users.getReferenceById(1L)).thenReturn(owner);
        when(inventoryBalances.lockByUserAndProduct(1L, 55L)).thenReturn(Optional.of(new InventoryBalance()));

        StockAdjustmentRequest request =
                new StockAdjustmentRequest(55L, new BigDecimal("2"), "Ajuste manual");

        inventoryService.adjustStock(1L, request);

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovements).save(movementCaptor.capture());
        InventoryMovement saved = movementCaptor.getValue();
        assertThat(saved.getSource()).isEqualTo("MANUAL_ADJUST");
        assertThat(saved.getReason()).isEqualTo("Ajuste manual");
        verify(auditService).log(eq(1L), eq("STOCK_ADJUST"), any(String.class));
    }

    @Test
    void listProductMovements_throwsWhenProductMissing() {
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                inventoryService.listProductMovements(1L, 9L, PageRequest.of(0, 20)))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(inventoryMovements);
    }

    @Test
    void listProductMovements_mapsPageForOwnedProduct() {
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(9L, 1L)).thenReturn(Optional.of(new Product()));
        InventoryMovement row = new InventoryMovement();
        row.setId(3L);
        row.setMovementType("ADJUSTMENT");
        row.setQuantity(new BigDecimal("-5"));
        row.setSource("MANUAL_ADJUST");
        row.setReason("quebra");
        Page<InventoryMovement> raw = new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1);
        when(inventoryMovements.findByUser_IdAndProduct_IdOrderByCreatedAtDesc(
                        eq(1L), eq(9L), any()))
                .thenReturn(raw);

        var page = inventoryService.listProductMovements(1L, 9L, PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).reason()).isEqualTo("quebra");
        assertThat(page.content().get(0).source()).isEqualTo("MANUAL_ADJUST");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void decreaseForSale_insufficientStock_throwsAndDoesNotPersistBalance() {
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(9L);

        InventoryBalance locked = new InventoryBalance();
        locked.setQuantity(new BigDecimal("2"));
        when(inventoryBalances.lockByUserAndProduct(1L, 9L)).thenReturn(Optional.of(locked));

        assertThatThrownBy(
                        () ->
                                inventoryService.decreaseForSale(
                                        user, product, new BigDecimal("3"), 77L))
                .isInstanceOf(InsufficientStockException.class)
                .satisfies(
                        ex ->
                                assertThat(((InsufficientStockException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));

        verify(inventoryMovements).save(any(InventoryMovement.class));
        verify(inventoryBalances, org.mockito.Mockito.never()).save(any(InventoryBalance.class));
    }

    @Test
    void assertSufficientStock_whenBelowRequested_throwsInsufficientStock() {
        InventoryBalance b = new InventoryBalance();
        b.setQuantity(new BigDecimal("1.5"));
        when(inventoryBalances.findByUser_IdAndProduct_Id(1L, 10L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> inventoryService.assertSufficientStock(1L, 10L, new BigDecimal("2")))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void increaseFromNfe_persistsEntryMovementAndUpdatesBalance() {
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(10L);

        InventoryBalance locked = new InventoryBalance();
        locked.setQuantity(new BigDecimal("5"));
        when(inventoryBalances.lockByUserAndProduct(1L, 10L)).thenReturn(Optional.of(locked));

        inventoryService.increaseFromNfe(user, product, new BigDecimal("2"), 99L);

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovements).save(movementCaptor.capture());
        InventoryMovement movement = movementCaptor.getValue();
        assertThat(movement.getMovementType()).isEqualTo("ENTRY");
        assertThat(movement.getQuantity()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(movement.getSource()).isEqualTo("NFE_IMPORT");
        assertThat(movement.getSourceId()).isEqualTo(99L);

        ArgumentCaptor<InventoryBalance> balanceCaptor = ArgumentCaptor.forClass(InventoryBalance.class);
        verify(inventoryBalances).save(balanceCaptor.capture());
        assertThat(balanceCaptor.getValue().getQuantity()).isEqualByComparingTo(new BigDecimal("7"));
    }

    @Test
    void inventoryKpis_mapsTotalStockValueFromProjection() {
        ProductRepository.InventoryKpiProjection projection =
                mock(ProductRepository.InventoryKpiProjection.class);
        when(projection.getTotalProducts()).thenReturn(2L);
        when(projection.getTotalUnits()).thenReturn(new BigDecimal("10"));
        when(projection.getLowStock()).thenReturn(1L);
        when(projection.getWithStock()).thenReturn(1L);
        when(projection.getTotalStockValue()).thenReturn(new BigDecimal("150.50"));
        when(products.calcInventoryKpis(1L)).thenReturn(projection);

        var kpis = inventoryService.inventoryKpis(1L);

        assertThat(kpis.totalSkus()).isEqualTo(2);
        assertThat(kpis.totalStockValue()).isEqualByComparingTo("150.50");
    }
}
