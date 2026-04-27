package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
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
import com.lojapp.application.inventory.AdjustInventoryUseCase;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock private AdjustInventoryUseCase adjustInventoryUseCase;

    @InjectMocks private InventoryService inventoryService;

    @BeforeEach
    void stubAdjustUseCaseDelegatesToManual() {
        lenient()
                .doAnswer(
                        inv -> {
                            inventoryService.applyManualStockAdjustment(
                                    inv.getArgument(0, Long.class),
                                    inv.getArgument(1, StockAdjustmentRequest.class));
                            return null;
                        })
                .when(adjustInventoryUseCase)
                .execute(anyLong(), any(), any());
    }

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
        when(products.findByIdAndUser_Id(9L, 1L)).thenReturn(Optional.empty());

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
        when(products.findByIdAndUser_Id(9L, 1L)).thenReturn(Optional.of(new Product()));
        when(inventoryBalances.findByUser_IdAndProduct_Id(1L, 9L)).thenReturn(Optional.empty());

        assertThat(inventoryService.getStockForOwnedProduct(1L, 9L)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getStockForOwnedProduct_returnsBalance() {
        when(products.findByIdAndUser_Id(9L, 1L)).thenReturn(Optional.of(new Product()));
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
        when(products.findByIdAndUser_Id(55L, 1L)).thenReturn(Optional.empty());
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(55L, new BigDecimal("2"), "Ajuste manual");

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, request))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(users, inventoryMovements, inventoryBalances, auditService);
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
}
