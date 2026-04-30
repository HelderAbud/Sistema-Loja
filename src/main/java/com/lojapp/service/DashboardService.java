package com.lojapp.service;

import com.lojapp.entity.Product;
import com.lojapp.config.CacheNames;
import com.lojapp.dto.dashboard.BrandDashboardResponse;
import com.lojapp.dto.dashboard.BrandKpiResponse;
import com.lojapp.dto.dashboard.ProductAbcResponse;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.dashboard.ProductAbcRowResponse;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.SaleRepository;
import com.lojapp.service.contract.DashboardServiceContract;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService implements DashboardServiceContract {

    private static final String FALLBACK_BRAND_NAME = "Nao informada";

    private final SaleRepository sales;
    private final int maxDashboardRangeDays;

    public DashboardService(
            SaleRepository sales,
            @Value("${lojapp.dashboard.max-range-days:366}") int maxDashboardRangeDays) {
        this.sales = sales;
        this.maxDashboardRangeDays = maxDashboardRangeDays;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.DASHBOARD_BRANDS,
            key =
                    "#userId + '|' + #from.toEpochMilli() + '|' + #to.toEpochMilli() + '|' + #brandLimit + '|'"
                            + " + #brandOffset")
    public BrandDashboardResponse brandDashboard(
            long userId, Instant from, Instant to, int brandLimit, int brandOffset) {
        assertDashboardWindow(from, to);
        if (brandLimit < 1 || brandLimit > 200) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST, "brandLimit deve estar entre 1 e 200");
        }
        if (brandOffset < 0) {
            throw new LojappDomainException(ApiErrorCode.BAD_REQUEST, "brandOffset nao pode ser negativo");
        }
        int pageIndex = brandOffset / brandLimit;
        int offsetInsidePage = brandOffset % brandLimit;
        int fetchSize = brandLimit + offsetInsidePage;
        List<SaleRepository.BrandKpiAggregateRow> rows =
                sales.aggregateBrandKpisPage(
                        userId, from, to, PageRequest.of(pageIndex, fetchSize));
        long total = sales.countBrandKpiGroups(userId, from, to);
        List<BrandKpiResponse> slice =
                rows.stream()
                        .skip(offsetInsidePage)
                        .map(
                                row ->
                                        toBrandKpi(
                                                resolveBrandName(row.getBrandId(), row.getBrandName()),
                                                row.getRevenue(),
                                                row.getProfit(),
                                                row.getQuantity()))
                        .collect(Collectors.toList());
        return new BrandDashboardResponse(from, to, slice, Math.toIntExact(total), brandLimit, brandOffset);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.DASHBOARD_PRODUCT_ABC, key = "#userId + '|' + #from.toEpochMilli() + '|' + #to.toEpochMilli()")
    public ProductAbcResponse productAbc(long userId, Instant from, Instant to) {
        assertDashboardWindow(from, to);
        List<SaleRepository.ProductAbcAggregateRow> byProduct = sales.aggregateProductAbc(userId, from, to);
        BigDecimal totalRevenue =
                byProduct.stream().map(SaleRepository.ProductAbcAggregateRow::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return new ProductAbcResponse(from, to, totalRevenue, List.of());
        }
        List<ProductAbcRowResponse> rows = new ArrayList<>();
        BigDecimal prevCumulative = BigDecimal.ZERO;
        for (SaleRepository.ProductAbcAggregateRow a : byProduct) {
            BigDecimal share =
                    a.getRevenue()
                            .multiply(new BigDecimal("100"))
                            .divide(totalRevenue, 2, RoundingMode.HALF_UP);
            BigDecimal cumulative = prevCumulative.add(share);
            String abc = classifyAbc(byProduct.size(), prevCumulative, cumulative);
            String brandName = a.getBrandName() == null ? FALLBACK_BRAND_NAME : a.getBrandName();
            rows.add(
                    new ProductAbcRowResponse(
                            a.getProductId(),
                            a.getProductName(),
                            brandName,
                            a.getRevenue(),
                            a.getQuantitySold(),
                            share,
                            cumulative,
                            abc));
            prevCumulative = cumulative;
        }
        return new ProductAbcResponse(from, to, totalRevenue, rows);
    }

    private void assertDashboardWindow(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST,
                    "Parametro 'from' deve ser anterior ou igual a 'to'");
        }
        long days = ChronoUnit.DAYS.between(from, to);
        if (days > maxDashboardRangeDays) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST,
                    "Intervalo maximo para relatorios do dashboard: %d dias (pedido: %d)."
                            .formatted(maxDashboardRangeDays, days));
        }
    }

    private static String classifyAbc(int skuCount, BigDecimal prevCumulative, BigDecimal cumulative) {
        if (skuCount == 1) {
            return "A";
        }
        if (prevCumulative.compareTo(new BigDecimal("80")) >= 0) {
            return prevCumulative.compareTo(new BigDecimal("95")) >= 0 ? "C" : "B";
        }
        return cumulative.compareTo(new BigDecimal("80")) <= 0 ? "A" : "B";
    }

    private static String resolveBrandName(Long brandId, String brandName) {
        if (brandId == null || brandName == null || brandName.isBlank()) {
            return FALLBACK_BRAND_NAME;
        }
        return brandName;
    }

    private BrandKpiResponse toBrandKpi(
            String brand, BigDecimal revenue, BigDecimal profit, BigDecimal quantity) {
        BigDecimal margin =
                revenue.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : profit
                                .multiply(new BigDecimal("100"))
                                .divide(revenue, 2, RoundingMode.HALF_UP);
        String giro =
                quantity.compareTo(new BigDecimal("100")) >= 0
                        ? "Alto"
                        : quantity.compareTo(new BigDecimal("40")) >= 0 ? "Medio" : "Baixo";
        String insight =
                switch (giro) {
                    case "Baixo" -> "Marca com baixo giro, revisar compras.";
                    case "Alto" -> "Marca com alto giro, considerar reposicao.";
                    default -> "Marca em giro medio, monitorar margem.";
                };
        return new BrandKpiResponse(brand, revenue, profit, quantity, margin, giro, insight);
    }
}
