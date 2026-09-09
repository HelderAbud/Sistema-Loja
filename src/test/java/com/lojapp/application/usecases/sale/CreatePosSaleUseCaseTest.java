package com.lojapp.application.usecases.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.application.contract.PosSaleCommissionServiceContract;
import com.lojapp.application.idempotency.ApiIdempotencyService;
import com.lojapp.dto.sale.PosSaleFinalizeRequest;
import com.lojapp.dto.sale.PosSaleFinalizeResponse;
import com.lojapp.dto.sale.PosSaleLineRequest;
import com.lojapp.dto.sale.PosSalePaymentRequest;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.entity.PaymentSettlementStatus;
import com.lojapp.entity.Product;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SaleItem;
import com.lojapp.entity.SalePayment;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.CashSessionNotOpenException;
import com.lojapp.exception.domain.PosSaleDuplicateProductException;
import com.lojapp.exception.domain.PosSalePaymentDetailsInvalidException;
import com.lojapp.exception.domain.PosSalePaymentTotalMismatchException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.SaleItemRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.contract.InventoryServiceContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreatePosSaleUseCaseTest {

    @Mock private UserRepository users;
    @Mock private ProductRepository products;
    @Mock private SaleRepository sales;
    @Mock private SaleItemRepository saleItems;
    @Mock private SalePaymentRepository salePayments;
    @Mock private CashSessionRepository cashSessions;
    @Mock private InventoryServiceContract inventoryService;
    @Mock private AuditService auditService;
    @Mock private ApiIdempotencyService idempotencyService;
    @Mock private PosSaleCommissionServiceContract posSaleCommissionService;

    private CreatePosSaleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase =
                new CreatePosSaleUseCase(
                        users,
                        products,
                        sales,
                        saleItems,
                        salePayments,
                        cashSessions,
                        inventoryService,
                        auditService,
                        idempotencyService,
                        posSaleCommissionService);
        lenient()
                .when(idempotencyService.runPosSaleFinalize(any(Long.class), any(), any(), any()))
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
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(product));
        when(cashSessions.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(session));

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
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
        CashSession session = new CashSession();
        session.setStatus(CashSessionStatus.CLOSED);

        when(users.getReferenceById(1L)).thenReturn(user);
        when(cashSessions.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(session));

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
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

    @Test
    void execute_whenTwoItemsAndPaymentsMatch_savesOneSaleAndDecreasesEachLine() {
        User user = new User();
        Product shirt = new Product();
        shirt.setId(10L);
        shirt.setCostPrice(new BigDecimal("5.00"));
        Product pants = new Product();
        pants.setId(11L);
        pants.setCostPrice(new BigDecimal("8.00"));
        CashSession session = new CashSession();
        session.setId(7L);
        session.setStatus(CashSessionStatus.OPEN);

        when(users.getReferenceById(1L)).thenReturn(user);
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(shirt));
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(11L, 1L)).thenReturn(Optional.of(pants));
        when(cashSessions.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(session));
        when(sales.save(any(Sale.class)))
                .thenAnswer(
                        inv -> {
                            Sale sale = inv.getArgument(0);
                            sale.setId(88L);
                            sale.setSoldAt(Instant.parse("2026-08-15T12:00:00Z"));
                            return sale;
                        });

        PosSaleFinalizeRequest request =
                new PosSaleFinalizeRequest(
                        7L,
                        null,
                        null,
                        null,
                        null,
                        List.of(
                                new PosSaleLineRequest(
                                        10L, new BigDecimal("2"), new BigDecimal("10.00"), new BigDecimal("5.00")),
                                new PosSaleLineRequest(
                                        11L, new BigDecimal("1"), new BigDecimal("20.00"), new BigDecimal("8.00"))),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CARD, new BigDecimal("40.00"))),
                        9L);

        PosSaleFinalizeResponse response = useCase.execute(1L, request, Optional.empty());

        assertThat(response.saleId()).isEqualTo(88L);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
        verify(saleItems, times(2)).save(any(SaleItem.class));
        verify(inventoryService)
                .decreaseForSale(
                        eq(user),
                        eq(shirt),
                        org.mockito.ArgumentMatchers.argThat(q -> q.compareTo(new BigDecimal("2")) == 0),
                        eq(88L));
        verify(inventoryService)
                .decreaseForSale(
                        eq(user),
                        eq(pants),
                        org.mockito.ArgumentMatchers.argThat(q -> q.compareTo(new BigDecimal("1")) == 0),
                        eq(88L));
        ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
        verify(sales, times(2)).save(saleCaptor.capture());
        assertThat(saleCaptor.getAllValues().get(0).getProduct()).isEqualTo(shirt);
        assertThat(saleCaptor.getAllValues().get(0).getQuantity())
                .isEqualByComparingTo(new BigDecimal("2"));
        verify(posSaleCommissionService)
                .assignSellerAndAccrue(eq(1L), eq(user), eq(session), any(Sale.class), any(), eq(9L));
    }

    @Test
    void execute_whenDuplicateProductInItems_throws() {
        PosSaleFinalizeRequest request =
                new PosSaleFinalizeRequest(
                        7L,
                        null,
                        null,
                        null,
                        null,
                        List.of(
                                new PosSaleLineRequest(
                                        10L, new BigDecimal("1"), new BigDecimal("10.00"), null),
                                new PosSaleLineRequest(
                                        10L, new BigDecimal("2"), new BigDecimal("10.00"), null)),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CASH, new BigDecimal("30.00"))),
                        null);

        assertThatThrownBy(() -> useCase.execute(1L, request, Optional.empty()))
                .isInstanceOf(PosSaleDuplicateProductException.class);

        verify(sales, never()).save(any());
    }

    @Test
    void execute_whenTwoItemsAndPaymentsDoNotMatch_throws() {
        User user = new User();
        Product shirt = new Product();
        shirt.setCostPrice(new BigDecimal("5.00"));
        Product pants = new Product();
        pants.setCostPrice(new BigDecimal("8.00"));
        CashSession session = new CashSession();
        session.setStatus(CashSessionStatus.OPEN);

        when(users.getReferenceById(1L)).thenReturn(user);
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(shirt));
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(11L, 1L)).thenReturn(Optional.of(pants));
        when(cashSessions.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(session));

        PosSaleFinalizeRequest request =
                new PosSaleFinalizeRequest(
                        7L,
                        null,
                        null,
                        null,
                        null,
                        List.of(
                                new PosSaleLineRequest(
                                        10L, new BigDecimal("1"), new BigDecimal("10.00"), new BigDecimal("5.00")),
                                new PosSaleLineRequest(
                                        11L, new BigDecimal("1"), new BigDecimal("20.00"), new BigDecimal("8.00"))),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CASH, new BigDecimal("25.00"))),
                        null);

        assertThatThrownBy(() -> useCase.execute(1L, request, Optional.empty()))
                .isInstanceOf(PosSalePaymentTotalMismatchException.class);

        verify(sales, never()).save(any());
    }

    @Test
    void execute_whenSplitCreditAndPix_persistsBothPaymentMethods() {
        User user = new User();
        Product product = new Product();
        product.setCostPrice(new BigDecimal("5.00"));
        CashSession session = new CashSession();
        session.setId(7L);
        session.setStatus(CashSessionStatus.OPEN);

        when(users.getReferenceById(1L)).thenReturn(user);
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(product));
        when(cashSessions.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(session));
        when(sales.save(any(Sale.class)))
                .thenAnswer(
                        inv -> {
                            Sale sale = inv.getArgument(0);
                            sale.setId(91L);
                            sale.setSoldAt(Instant.parse("2026-09-09T12:00:00Z"));
                            return sale;
                        });

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
                        7L,
                        10L,
                        new BigDecimal("1"),
                        new BigDecimal("50.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CREDIT_CARD, new BigDecimal("30.00")),
                                new PosSalePaymentRequest(PaymentMethod.PIX, new BigDecimal("20.00"))));

        useCase.execute(1L, request, Optional.empty());

        ArgumentCaptor<SalePayment> paymentCaptor = ArgumentCaptor.forClass(SalePayment.class);
        verify(salePayments, times(2)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getAllValues())
                .extracting(SalePayment::getPaymentMethod)
                .containsExactly(PaymentMethod.CREDIT_CARD, PaymentMethod.PIX);
    }

    @Test
    void execute_whenCashReceivedAmount_persistsChangeAndDetails() {
        stubOpenSessionAndProduct();
        stubSaleSave();

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
                        7L,
                        10L,
                        new BigDecimal("1"),
                        new BigDecimal("80.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CASH,
                                        new BigDecimal("80.00"),
                                        null,
                                        null,
                                        null,
                                        null,
                                        new BigDecimal("100.00"))));

        PosSaleFinalizeResponse response = useCase.execute(1L, request, Optional.empty());

        assertThat(response.changeAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        ArgumentCaptor<SalePayment> paymentCaptor = ArgumentCaptor.forClass(SalePayment.class);
        verify(salePayments).save(paymentCaptor.capture());
        SalePayment saved = paymentCaptor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(saved.getReceivedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void execute_whenCreditAndPixDetails_persistsBrandInstallmentsAndEndToEnd() {
        stubOpenSessionAndProduct();
        stubSaleSave();

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
                        7L,
                        10L,
                        new BigDecimal("1"),
                        new BigDecimal("50.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CREDIT_CARD,
                                        new BigDecimal("30.00"),
                                        "VISA",
                                        3,
                                        "NSU-1",
                                        null,
                                        null),
                                new PosSalePaymentRequest(
                                        PaymentMethod.PIX,
                                        new BigDecimal("20.00"),
                                        null,
                                        null,
                                        null,
                                        "E2E1234567890123456789012345678",
                                        null)));

        PosSaleFinalizeResponse response = useCase.execute(1L, request, Optional.empty());
        assertThat(response.changeAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        ArgumentCaptor<SalePayment> paymentCaptor = ArgumentCaptor.forClass(SalePayment.class);
        verify(salePayments, times(2)).save(paymentCaptor.capture());
        SalePayment card = paymentCaptor.getAllValues().get(0);
        SalePayment pix = paymentCaptor.getAllValues().get(1);
        assertThat(card.getCardBrand()).isEqualTo("VISA");
        assertThat(card.getInstallments()).isEqualTo(3);
        assertThat(card.getTransactionId()).isEqualTo("NSU-1");
        assertThat(pix.getEndToEndId()).isEqualTo("E2E1234567890123456789012345678");
    }

    @Test
    void execute_whenReceivedAmountOnPix_throws() {
        stubOpenSessionAndProduct();

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
                        7L,
                        10L,
                        new BigDecimal("1"),
                        new BigDecimal("20.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.PIX,
                                        new BigDecimal("20.00"),
                                        null,
                                        null,
                                        null,
                                        null,
                                        new BigDecimal("20.00"))));

        assertThatThrownBy(() -> useCase.execute(1L, request, Optional.empty()))
                .isInstanceOf(PosSalePaymentDetailsInvalidException.class);
        verify(sales, never()).save(any());
    }

    @Test
    void execute_whenReceivedAmountLessThanAmount_throws() {
        stubOpenSessionAndProduct();

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
                        7L,
                        10L,
                        new BigDecimal("1"),
                        new BigDecimal("80.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CASH,
                                        new BigDecimal("80.00"),
                                        null,
                                        null,
                                        null,
                                        null,
                                        new BigDecimal("50.00"))));

        assertThatThrownBy(() -> useCase.execute(1L, request, Optional.empty()))
                .isInstanceOf(PosSalePaymentDetailsInvalidException.class);
        verify(sales, never()).save(any());
    }

    @Test
    void execute_whenInstallmentsOnCash_throws() {
        stubOpenSessionAndProduct();

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
                        7L,
                        10L,
                        new BigDecimal("1"),
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CASH,
                                        new BigDecimal("10.00"),
                                        null,
                                        2,
                                        null,
                                        null,
                                        null)));

        assertThatThrownBy(() -> useCase.execute(1L, request, Optional.empty()))
                .isInstanceOf(PosSalePaymentDetailsInvalidException.class);
        verify(sales, never()).save(any());
    }

    @Test
    void execute_whenPixPending_persistsPendingSettlement() {
        stubOpenSessionAndProduct();
        stubSaleSave();

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
                        7L,
                        10L,
                        new BigDecimal("1"),
                        new BigDecimal("20.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.PIX,
                                        new BigDecimal("20.00"),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        PaymentSettlementStatus.PENDING)));

        useCase.execute(1L, request, Optional.empty());

        ArgumentCaptor<SalePayment> paymentCaptor = ArgumentCaptor.forClass(SalePayment.class);
        verify(salePayments).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getSettlementStatus())
                .isEqualTo(PaymentSettlementStatus.PENDING);
        assertThat(paymentCaptor.getValue().getSettlementCashSession()).isNull();
    }

    @Test
    void execute_whenCashPending_throws() {
        stubOpenSessionAndProduct();

        PosSaleFinalizeRequest request =
                PosSaleFinalizeRequest.singleItem(
                        7L,
                        10L,
                        new BigDecimal("1"),
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00"),
                        List.of(
                                new PosSalePaymentRequest(
                                        PaymentMethod.CASH,
                                        new BigDecimal("10.00"),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        PaymentSettlementStatus.PENDING)));

        assertThatThrownBy(() -> useCase.execute(1L, request, Optional.empty()))
                .isInstanceOf(PosSalePaymentDetailsInvalidException.class);
        verify(sales, never()).save(any());
    }

    private void stubOpenSessionAndProduct() {
        User user = new User();
        Product product = new Product();
        product.setCostPrice(new BigDecimal("5.00"));
        CashSession session = new CashSession();
        session.setId(7L);
        session.setStatus(CashSessionStatus.OPEN);

        when(users.getReferenceById(1L)).thenReturn(user);
        when(products.findByIdAndUser_IdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(product));
        when(cashSessions.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.of(session));
    }

    private void stubSaleSave() {
        when(sales.save(any(Sale.class)))
                .thenAnswer(
                        inv -> {
                            Sale sale = inv.getArgument(0);
                            sale.setId(91L);
                            sale.setSoldAt(Instant.parse("2026-09-09T12:00:00Z"));
                            return sale;
                        });
    }
}
