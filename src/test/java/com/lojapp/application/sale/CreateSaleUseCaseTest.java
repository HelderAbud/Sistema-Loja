package com.lojapp.application.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lojapp.application.idempotency.ApiIdempotencyService;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.sale.SaleCreatedResponse;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.entity.Product;
import com.lojapp.entity.Sale;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.InsufficientStockException;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.ProductNotFoundException;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.observability.LojappBusinessMetrics;
import com.lojapp.service.AuditService;
import com.lojapp.service.contract.InventoryServiceContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateSaleUseCaseTest {

    @Mock private UserRepository users;
    @Mock private ProductRepository products;
    @Mock private SaleRepository sales;
    @Mock private InventoryServiceContract inventoryService;
    @Mock private AuditService auditService;
    @Mock private ApiIdempotencyService idempotencyService;
    @Mock private LojappBusinessMetrics businessMetrics;

    private CreateSaleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase =
                new CreateSaleUseCase(
                        users,
                        products,
                        sales,
                        inventoryService,
                        auditService,
                        idempotencyService,
                        businessMetrics);
        when(idempotencyService.runSaleCreate(anyLong(), any(), anyString(), any()))
                .thenAnswer(
                        inv -> {
                            @SuppressWarnings("unchecked")
                            Supplier<SaleCreatedResponse> supplier =
                                    (Supplier<SaleCreatedResponse>) inv.getArgument(3);
                            return supplier.get();
                        });
    }

    @Test
    void negativeResolvedUnitCost_rejects() {
        long userId = 1L;
        User userRef = new User();
        when(users.getReferenceById(userId)).thenReturn(userRef);

        Product product = new Product();
        product.setId(10L);
        product.setCostPrice(new BigDecimal("-1.00"));
        when(products.findByIdAndUser_Id(10L, userId)).thenReturn(Optional.of(product));

        SaleRequest request =
                new SaleRequest(10L, new BigDecimal("1"), new BigDecimal("5.00"), null);

        assertThatThrownBy(() -> useCase.execute(userId, request, Optional.empty()))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void persistsSaleAndDecreasesStock() {
        long userId = 2L;
        User userRef = new User();
        when(users.getReferenceById(userId)).thenReturn(userRef);

        Product product = new Product();
        product.setId(20L);
        product.setCostPrice(new BigDecimal("3.00"));
        when(products.findByIdAndUser_Id(20L, userId)).thenReturn(Optional.of(product));

        when(sales.save(any(Sale.class)))
                .thenAnswer(
                        inv -> {
                            Sale s = inv.getArgument(0);
                            s.setId(77L);
                            s.setSoldAt(Instant.parse("2026-04-01T10:00:00Z"));
                            return s;
                        });

        SaleCreatedResponse created =
                useCase.execute(
                        userId,
                        new SaleRequest(20L, new BigDecimal("4"), new BigDecimal("9.99"), null),
                        Optional.empty());

        assertThat(created.id()).isEqualTo(77L);

        ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
        verify(sales).save(saleCaptor.capture());
        Sale saved = saleCaptor.getValue();
        assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("4"));
        assertThat(saved.getUnitPrice()).isEqualByComparingTo(new BigDecimal("9.99"));

        verify(inventoryService)
                .decreaseForSale(
                        eq(userRef),
                        eq(product),
                        org.mockito.ArgumentMatchers.argThat(
                                q -> q.compareTo(new BigDecimal("4")) == 0),
                        eq(77L));
    }

    @Test
    void whenProductDoesNotBelongToUser_throwsNotFound() {
        long userId = 3L;
        User userRef = new User();
        when(users.getReferenceById(userId)).thenReturn(userRef);
        when(products.findByIdAndUser_Id(99L, userId)).thenReturn(Optional.empty());

        SaleRequest request =
                new SaleRequest(99L, new BigDecimal("1"), new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> useCase.execute(userId, request, Optional.empty()))
                .isInstanceOf(ProductNotFoundException.class)
                .satisfies(
                        ex ->
                                assertThat(((ProductNotFoundException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.NOT_FOUND));

        verifyNoInteractions(sales, inventoryService, auditService);
    }

    @Test
    void withExplicitUnitCost_usesRequestCostAndWritesAudit() {
        long userId = 4L;
        User userRef = new User();
        userRef.setId(userId);
        when(users.getReferenceById(userId)).thenReturn(userRef);

        Product product = new Product();
        product.setId(40L);
        product.setCostPrice(new BigDecimal("2.50"));
        when(products.findByIdAndUser_Id(40L, userId)).thenReturn(Optional.of(product));

        when(sales.save(any(Sale.class)))
                .thenAnswer(
                        inv -> {
                            Sale s = inv.getArgument(0);
                            s.setId(401L);
                            s.setSoldAt(Instant.parse("2026-04-02T12:00:00Z"));
                            return s;
                        });

        SaleRequest request =
                new SaleRequest(40L, new BigDecimal("2"), new BigDecimal("12.00"), new BigDecimal("4.40"));
        SaleCreatedResponse created = useCase.execute(userId, request, Optional.empty());

        assertThat(created.unitCost()).isEqualByComparingTo(new BigDecimal("4.40"));
        verify(auditService)
                .log(
                        eq(userId),
                        eq("SALE_CREATED"),
                        org.mockito.ArgumentMatchers.argThat(
                                details ->
                                        details.contains("saleId=401")
                                                && details.contains("productId=40")
                                                && details.contains("qty=2")));
    }

    @Test
    void whenInventoryRejects_delegatesInsufficientStock() {
        long userId = 5L;
        User userRef = new User();
        when(users.getReferenceById(userId)).thenReturn(userRef);

        Product product = new Product();
        product.setId(50L);
        product.setCostPrice(new BigDecimal("2.00"));
        when(products.findByIdAndUser_Id(50L, userId)).thenReturn(Optional.of(product));

        when(sales.save(any(Sale.class)))
                .thenAnswer(
                        inv -> {
                            Sale s = inv.getArgument(0);
                            s.setId(500L);
                            s.setSoldAt(Instant.parse("2026-04-10T08:00:00Z"));
                            return s;
                        });

        doThrow(new InsufficientStockException())
                .when(inventoryService)
                .decreaseForSale(any(), any(), any(), org.mockito.ArgumentMatchers.eq(500L));

        SaleRequest request =
                new SaleRequest(50L, new BigDecimal("1"), new BigDecimal("8.00"), null);

        assertThatThrownBy(() -> useCase.execute(userId, request, Optional.empty()))
                .isInstanceOf(InsufficientStockException.class);
    }
}
