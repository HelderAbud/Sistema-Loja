package com.lojapp.service;

import com.lojapp.application.usecases.sale.CancelSaleUseCase;
import com.lojapp.application.usecases.sale.CreateSaleUseCase;
import com.lojapp.dto.sale.SaleListItemResponse;
import com.lojapp.dto.sale.SaleCreatedResponse;
import com.lojapp.dto.sale.SalesDailyPointResponse;
import com.lojapp.dto.sale.SalePageResponse;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.dto.sale.SalesPaymentsSummaryResponse;
import com.lojapp.dto.sale.SalesSummaryResponse;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.entity.PaymentMethod;
import com.lojapp.entity.PaymentSettlementStatus;
import com.lojapp.entity.Sale;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.application.contract.SalesServiceContract;
import com.lojapp.util.Pageables;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesService implements SalesServiceContract {

    private final SaleRepository sales;
    private final SalePaymentRepository salePayments;
    private final CreateSaleUseCase createSaleUseCase;
    private final CancelSaleUseCase cancelSaleUseCase;

    public SalesService(
            SaleRepository sales,
            SalePaymentRepository salePayments,
            CreateSaleUseCase createSaleUseCase,
            CancelSaleUseCase cancelSaleUseCase) {
        this.sales = sales;
        this.salePayments = salePayments;
        this.createSaleUseCase = createSaleUseCase;
        this.cancelSaleUseCase = cancelSaleUseCase;
    }

    @Override
    public SaleCreatedResponse registerSale(long userId, SaleRequest request) {
        return registerSale(userId, request, Optional.empty());
    }

    @Override
    public SaleCreatedResponse registerSale(
            long userId, SaleRequest request, Optional<String> idempotencyKeyHeader) {
        return createSaleUseCase.execute(userId, request, idempotencyKeyHeader);
    }

    @Override
    @Transactional
    public void cancelSale(long userId, long saleId) {
        cancelSaleUseCase.execute(userId, saleId);
    }

    @Transactional(readOnly = true)
    public SalePageResponse listSales(
            long userId, Instant from, Instant to, Long productId, Long brandId, Pageable pageable) {
        Instant end = resolveEnd(to);
        Instant start = resolveStart(from, end);
        validateRange(start, end);
        Page<Sale> page = sales.searchForUser(userId, start, end, productId, brandId, Pageables.clamp(pageable));
        return SalePageResponse.from(page.map(SaleListItemResponse::from));
    }

    @Transactional(readOnly = true)
    public SalePageResponse listSales(
            long userId, Instant from, Instant to, Long productId, Pageable pageable) {
        return listSales(userId, from, to, productId, null, pageable);
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse summarizeSales(
            long userId, Instant from, Instant to, Long productId, Long brandId) {
        Instant end = resolveEnd(to);
        Instant start = resolveStart(from, end);
        validateRange(start, end);
        SaleRepository.SalesSummaryAggregateRow row = sales.aggregateSalesSummary(userId, start, end, productId, brandId);
        return new SalesSummaryResponse(row.getRevenue(), row.getUnitsSold(), row.getAverageTicket());
    }

    @Transactional(readOnly = true)
    public List<SalesDailyPointResponse> summarizeSalesDaily(
            long userId, Instant from, Instant to, Long productId, Long brandId) {
        Instant end = resolveEnd(to);
        Instant start = resolveStart(from, end);
        validateRange(start, end);
        return sales.aggregateSalesDaily(userId, start, end, productId, brandId).stream()
                .map(
                        row ->
                                new SalesDailyPointResponse(
                                        row.getSoldDate(), row.getRevenue(), row.getUnitsSold()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesPaymentsSummaryResponse summarizeSalesPayments(
            long userId, Instant from, Instant to) {
        Instant end = resolveEnd(to);
        Instant start = resolveStart(from, end);
        validateRange(start, end);
        return new SalesPaymentsSummaryResponse(
                toSlice(salePayments.aggregateBySoldAt(userId, start, end)),
                toSlice(salePayments.aggregateConfirmedBySettledAt(userId, start, end)),
                toSlice(salePayments.aggregateOpenPending(userId)));
    }

    private static SalesPaymentsSummaryResponse.Slice toSlice(
            List<SalePaymentRepository.PaymentSettlementAggregateRow> rows) {
        EnumMap<PaymentMethod, BigDecimal> confirmed = new EnumMap<>(PaymentMethod.class);
        EnumMap<PaymentMethod, BigDecimal> pending = new EnumMap<>(PaymentMethod.class);
        for (SalePaymentRepository.PaymentSettlementAggregateRow row : rows) {
            BigDecimal amount = row.getAmount() == null ? BigDecimal.ZERO : row.getAmount();
            EnumMap<PaymentMethod, BigDecimal> bucket =
                    row.getSettlementStatus() == PaymentSettlementStatus.PENDING
                            ? pending
                            : confirmed;
            bucket.merge(row.getPaymentMethod(), amount, BigDecimal::add);
        }
        EnumSet<PaymentMethod> methods = EnumSet.noneOf(PaymentMethod.class);
        methods.addAll(confirmed.keySet());
        methods.addAll(pending.keySet());
        List<SalesPaymentsSummaryResponse.PaymentMethodBreakdown> breakdown =
                methods.stream()
                        .sorted(Comparator.comparing(Enum::name))
                        .map(
                                method ->
                                        new SalesPaymentsSummaryResponse.PaymentMethodBreakdown(
                                                method,
                                                confirmed.getOrDefault(method, BigDecimal.ZERO),
                                                pending.getOrDefault(method, BigDecimal.ZERO)))
                        .toList();
        return new SalesPaymentsSummaryResponse.Slice(
                confirmed.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                pending.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                breakdown);
    }

    private Instant resolveEnd(Instant to) {
        return to == null ? Instant.now() : to;
    }

    private Instant resolveStart(Instant from, Instant end) {
        return from == null ? end.minus(30, ChronoUnit.DAYS) : from;
    }

    private void validateRange(Instant start, Instant end) {
        if (start.isAfter(end)) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST,
                    "Parametro 'from' deve ser anterior ou igual a 'to'");
        }
    }
}
