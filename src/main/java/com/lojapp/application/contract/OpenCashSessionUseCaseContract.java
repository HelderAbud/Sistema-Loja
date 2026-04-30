package com.lojapp.application.contract;

import com.lojapp.dto.cash.OpenCashSessionRequest;
import com.lojapp.dto.cash.OpenCashSessionResponse;

public interface OpenCashSessionUseCaseContract {

    OpenCashSessionResponse execute(long userId, long actorUserId, OpenCashSessionRequest request);
}
