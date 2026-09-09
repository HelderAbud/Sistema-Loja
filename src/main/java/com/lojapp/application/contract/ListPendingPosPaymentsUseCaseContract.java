package com.lojapp.application.contract;

import com.lojapp.dto.sale.PosSalePaymentView;
import java.util.List;

public interface ListPendingPosPaymentsUseCaseContract {

    List<PosSalePaymentView> execute(long userId);
}
