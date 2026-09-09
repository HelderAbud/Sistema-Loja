package com.lojapp.application.usecases.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lojapp.dto.sale.PosSalePaymentView;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.entity.PaymentSettlementStatus;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SalePayment;
import com.lojapp.repository.SalePaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListPendingPosPaymentsUseCaseTest {

    @Mock private SalePaymentRepository salePayments;

    private ListPendingPosPaymentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListPendingPosPaymentsUseCase(salePayments);
    }

    @Test
    void execute_mapsPendingPayments() {
        CashSession session = new CashSession();
        session.setId(7L);
        session.setStatus(CashSessionStatus.OPEN);
        Sale sale = new Sale();
        sale.setId(91L);
        sale.setCashSession(session);
        SalePayment payment = new SalePayment();
        payment.setId(5L);
        payment.setSale(sale);
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setAmount(new BigDecimal("20.00"));
        payment.setSettlementStatus(PaymentSettlementStatus.PENDING);
        when(salePayments.findPendingUncancelled(1L)).thenReturn(List.of(payment));

        List<PosSalePaymentView> views = useCase.execute(1L);

        assertThat(views)
                .containsExactly(
                        new PosSalePaymentView(
                                5L,
                                91L,
                                7L,
                                null,
                                PaymentMethod.PIX,
                                new BigDecimal("20.00"),
                                PaymentSettlementStatus.PENDING));
    }
}
