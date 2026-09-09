package com.lojapp.application.contract;

import com.lojapp.dto.sale.PosSalePaymentView;

public interface ConfirmPosSalePaymentUseCaseContract {

    PosSalePaymentView execute(long userId, long saleId, long paymentId);
}
