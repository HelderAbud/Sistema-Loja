package com.lojapp.application.contract;

import com.lojapp.dto.cash.CurrentCashSessionResponse;

public interface GetCurrentCashSessionUseCaseContract {

    CurrentCashSessionResponse execute(long userId);
}
