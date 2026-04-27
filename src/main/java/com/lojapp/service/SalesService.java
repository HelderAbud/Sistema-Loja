package com.lojapp.service;



import com.lojapp.application.sale.CancelSaleUseCase;
import com.lojapp.application.sale.CreateSaleUseCase;

import com.lojapp.dto.sale.SaleListItemResponse;

import com.lojapp.dto.sale.SaleCreatedResponse;

import com.lojapp.dto.sale.SalesDailyPointResponse;

import com.lojapp.dto.sale.SalePageResponse;

import com.lojapp.dto.sale.SaleRequest;

import com.lojapp.dto.sale.SalesSummaryResponse;

import com.lojapp.dto.ApiErrorCode;

import com.lojapp.entity.Sale;

import com.lojapp.exception.domain.LojappDomainException;

import com.lojapp.repository.SaleRepository;

import com.lojapp.service.contract.SalesServiceContract;

import com.lojapp.util.Pageables;

import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.List;

import java.util.Optional;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



@Service

public class SalesService implements SalesServiceContract {



    private final SaleRepository sales;

    private final CreateSaleUseCase createSaleUseCase;

    private final CancelSaleUseCase cancelSaleUseCase;

    public SalesService(
            SaleRepository sales,
            CreateSaleUseCase createSaleUseCase,
            CancelSaleUseCase cancelSaleUseCase) {

        this.sales = sales;

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

        Page<Sale> page =

                sales.searchForUser(userId, start, end, productId, brandId, Pageables.clamp(pageable));

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



        SaleRepository.SalesSummaryAggregateRow row =

                sales.aggregateSalesSummary(userId, start, end, productId, brandId);

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

