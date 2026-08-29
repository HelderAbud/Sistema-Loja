package com.lojapp.service.contract;

import com.lojapp.dto.commission.CommissionAccrualResponse;
import java.time.Instant;
import java.util.List;

public interface CommissionReportServiceContract {

    List<CommissionAccrualResponse> list(long userId, Instant from, Instant to);

    String toCsv(long userId, Instant from, Instant to);
}
