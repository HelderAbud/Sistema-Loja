package com.lojapp.application.contract;

import com.lojapp.dto.cash.CloseCashSessionRequest;
import com.lojapp.dto.cash.CloseCashSessionResponse;

public interface CloseCashSessionUseCaseContract {

    CloseCashSessionResponse execute(long userId, long actorUserId, CloseCashSessionRequest request);
}
