package com.lojapp.application.contract;

import com.lojapp.dto.sale.PosSaleFinalizeRequest;
import com.lojapp.dto.sale.PosSaleFinalizeResponse;
import java.util.Optional;

public interface CreatePosSaleUseCaseContract {

    PosSaleFinalizeResponse execute(
            long userId, PosSaleFinalizeRequest request, Optional<String> idempotencyKeyHeader);
}
