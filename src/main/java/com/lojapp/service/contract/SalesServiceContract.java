package com.lojapp.service.contract;



import com.lojapp.dto.sale.SaleCreatedResponse;

import com.lojapp.dto.sale.SalesDailyPointResponse;

import com.lojapp.dto.sale.SalePageResponse;

import com.lojapp.dto.sale.SaleRequest;

import com.lojapp.dto.sale.SalesSummaryResponse;

import java.time.Instant;

import java.util.List;

import java.util.Optional;

import org.springframework.data.domain.Pageable;



public interface SalesServiceContract {



    default SaleCreatedResponse registerSale(long userId, SaleRequest request) {

        return registerSale(userId, request, Optional.empty());

    }



    SaleCreatedResponse registerSale(

            long userId, SaleRequest request, Optional<String> idempotencyKeyHeader);

    /** Cancela venda, repõe stock e exclui a linha dos totais do dashboard. */
    void cancelSale(long userId, long saleId);

    SalePageResponse listSales(

            long userId, Instant from, Instant to, Long productId, Long brandId, Pageable pageable);



    SalesSummaryResponse summarizeSales(long userId, Instant from, Instant to, Long productId, Long brandId);



    List<SalesDailyPointResponse> summarizeSalesDaily(

            long userId, Instant from, Instant to, Long productId, Long brandId);

}

