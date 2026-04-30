package com.lojapp.application.contract;

import com.lojapp.dto.cash.CloseCashSessionPreviewResponse;
import java.math.BigDecimal;

public interface GetCashSessionClosePreviewUseCaseContract {

    CloseCashSessionPreviewResponse execute(long userId, long cashSessionId, BigDecimal countedAmount);
}
