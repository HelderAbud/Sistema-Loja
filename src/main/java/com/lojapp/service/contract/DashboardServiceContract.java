package com.lojapp.service.contract;

import com.lojapp.dto.dashboard.BrandDashboardResponse;
import com.lojapp.dto.dashboard.ProductAbcResponse;
import java.time.Instant;

public interface DashboardServiceContract {

    BrandDashboardResponse brandDashboard(
            long userId, Instant from, Instant to, int brandLimit, int brandOffset);

    ProductAbcResponse productAbc(long userId, Instant from, Instant to);
}
