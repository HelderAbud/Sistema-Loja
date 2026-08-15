package com.lojapp.application.sale;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.entity.Product;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SaleItem;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.SaleAlreadyCancelledException;
import com.lojapp.repository.SaleItemRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.contract.InventoryServiceContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelSaleUseCaseTest {

    @Mock private SaleRepository sales;
    @Mock private SaleItemRepository saleItems;
    @Mock private InventoryServiceContract inventoryService;
    @Mock private AuditService auditService;

    private CancelSaleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelSaleUseCase(sales, saleItems, inventoryService, auditService);
    }

    @Test
    void execute_whenMultipleItems_restoresEachLine() {
        User user = new User();
        user.setId(1L);
        Product shirt = new Product();
        shirt.setId(10L);
        Product pants = new Product();
        pants.setId(11L);
        Sale sale = new Sale();
        sale.setId(50L);
        sale.setUser(user);
        sale.setProduct(shirt);
        sale.setQuantity(new BigDecimal("2"));

        SaleItem line1 = new SaleItem();
        line1.setProduct(shirt);
        line1.setQuantity(new BigDecimal("2"));
        SaleItem line2 = new SaleItem();
        line2.setProduct(pants);
        line2.setQuantity(new BigDecimal("1"));

        when(sales.findByIdAndUser_Id(50L, 1L)).thenReturn(Optional.of(sale));
        when(saleItems.findBySale_IdAndUser_Id(50L, 1L)).thenReturn(List.of(line1, line2));

        useCase.execute(1L, 50L);

        verify(inventoryService)
                .restoreStockForCancelledSale(eq(user), eq(shirt), eq(new BigDecimal("2")), eq(50L));
        verify(inventoryService)
                .restoreStockForCancelledSale(eq(user), eq(pants), eq(new BigDecimal("1")), eq(50L));
        verify(auditService).log(eq(1L), eq("SALE_CANCELLED"), any());
    }

    @Test
    void execute_whenAlreadyCancelled_doesNotRestore() {
        Sale sale = new Sale();
        sale.setId(9L);
        sale.setCancelledAt(Instant.parse("2026-08-01T10:00:00Z"));
        when(sales.findByIdAndUser_Id(9L, 1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> useCase.execute(1L, 9L)).isInstanceOf(SaleAlreadyCancelledException.class);

        verify(inventoryService, never()).restoreStockForCancelledSale(any(), any(), any(), anyLong());
    }
}
