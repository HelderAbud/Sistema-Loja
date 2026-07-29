package com.lojapp.application.sale;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.application.idempotency.ApiIdempotencyService;
import com.lojapp.dto.sale.PosSaleFinalizeRequest;
import com.lojapp.dto.sale.PosSaleFinalizeResponse;
import com.lojapp.dto.sale.PosSalePaymentRequest;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.entity.Product;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.CashSessionNotOpenException;
import com.lojapp.exception.domain.PosSalePaymentTotalMismatchException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.contract.InventoryServiceContract;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreatePosSaleUseCaseTest {

    @Mock private UserRepository users;
    @Mock private ProductRepository products;
    @Mock private SaleRepository sales;
    @Mock private SalePaymentRepository salePayments;
    @Mock private CashSessionRepository cashSessions;
    @Mock private InventoryServiceContract inventoryService;
    @Mock private AuditService auditService;
    @Mock private ApiIdempotencyService idempotencyService;

    private CreatePosSaleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase =
                new CreatePosSaleUseCase(
                        users,
                        products,
                        sales,
                        salePayments,
                        cashSessions,
                        inventoryService,
                        auditService,
                        idempotencyService);
        when(idempotencyService.runPosSaleFinalize(any(Long.class), any(), any(), any()))
                .thenAnswer(
                        inv -> {
                            @SuppressWarnings("unchecked")
                            Supplier<PosSaleFinalizeResponse> supplier = inv.getArgument(3);
                            return supplier.get();
                        });
    }

    @Test
    void execute_whenPaymentsDoNotMatchSaleTotal_throws() {
        User user = new User();
        Product product = new Product();
        product.setCostPrice(new BigDecimal("5.00"));
        CashSession session = new CashSession();
        session.setStatus(CashSessionStatus.OPEN);

        when(users.getReferenceById(1L)).thenReturn(user);
        when(products.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(product));
        when(cashSessions.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(session));

        PosSaleFinalizeRequest request =
                new PosSaleFinalizeRequest(
                        7L,
                        10L,
                        new BigDecimal("2"),
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CASH, new BigDecimal("15.00"))));

        assertThatThrownBy(() -> useCase.execute(1L, request, Optional.empty()))
                .isInstanceOf(PosSalePaymentTotalMismatchException.class);

        verify(sales, never()).save(any());
    }

    @Test
    void execute_whenCashSessionClosed_throws() {
        User user = new User();
        Product product = new Product();
        product.setCostPrice(new BigDecimal("5.00"));
        CashSession session = new CashSession();
        session.setStatus(CashSessionStatus.CLOSED);

        when(users.getReferenceById(1L)).thenReturn(user);
        when(products.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(product));
        when(cashSessions.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(session));

        PosSaleFinalizeRequest request =
                new PosSaleFinalizeRequest(
                        7L,
                        10L,
                        new BigDecimal("1"),
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CASH, new BigDecimal("10.00"))));

        assertThatThrownBy(() -> useCase.execute(1L, request, Optional.empty()))
                .isInstanceOf(CashSessionNotOpenException.class);

        verify(sales, never()).save(any());
    }
}
