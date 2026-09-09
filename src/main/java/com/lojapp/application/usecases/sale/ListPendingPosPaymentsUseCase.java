package com.lojapp.application.usecases.sale;

import com.lojapp.application.contract.ListPendingPosPaymentsUseCaseContract;
import com.lojapp.dto.sale.PosSalePaymentView;
import com.lojapp.repository.SalePaymentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPendingPosPaymentsUseCase implements ListPendingPosPaymentsUseCaseContract {

    private final SalePaymentRepository salePayments;

    public ListPendingPosPaymentsUseCase(SalePaymentRepository salePayments) {
        this.salePayments = salePayments;
    }

    @Transactional(readOnly = true)
    public List<PosSalePaymentView> execute(long userId) {
        return salePayments.findPendingUncancelled(userId).stream()
                .map(PosSalePaymentView::from)
                .toList();
    }
}
