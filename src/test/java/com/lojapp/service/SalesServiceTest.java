package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lojapp.application.sale.CancelSaleUseCase;
import com.lojapp.application.sale.CreateSaleUseCase;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.sale.SaleCreatedResponse;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock private SaleRepository sales;
    @Mock private CreateSaleUseCase createSaleUseCase;
    @Mock private CancelSaleUseCase cancelSaleUseCase;

    @InjectMocks private SalesService salesService;

    @Test
    void registerSale_delegatesToCreateSaleUseCase() {
        long userId = 1L;
        SaleRequest request =
                new SaleRequest(5L, new BigDecimal("1"), new BigDecimal("10.00"), null);
        SaleCreatedResponse expected =
                new SaleCreatedResponse(
                        9L, 5L, new BigDecimal("1"), new BigDecimal("10.00"), new BigDecimal("3.00"), Instant.now());
        when(createSaleUseCase.execute(userId, request, Optional.empty())).thenReturn(expected);

        assertThat(salesService.registerSale(userId, request)).isEqualTo(expected);

        verify(createSaleUseCase).execute(userId, request, Optional.empty());
    }

    @Test
    void registerSale_passesIdempotencyHeader() {
        long userId = 2L;
        SaleRequest request =
                new SaleRequest(5L, new BigDecimal("1"), new BigDecimal("10.00"), null);
        SaleCreatedResponse expected =
                new SaleCreatedResponse(
                        9L, 5L, new BigDecimal("1"), new BigDecimal("10.00"), new BigDecimal("3.00"), Instant.now());
        when(createSaleUseCase.execute(userId, request, Optional.of("k1"))).thenReturn(expected);

        assertThat(salesService.registerSale(userId, request, Optional.of("k1"))).isEqualTo(expected);
    }

    @Test
    void listSales_fromAfterTo_rejectsWithBadRequest() {
        long userId = 12L;
        Instant from = Instant.parse("2026-03-31T00:00:00Z");
        Instant to = Instant.parse("2026-03-01T00:00:00Z");

        assertThatThrownBy(
                        () ->
                                salesService.listSales(
                                        userId,
                                        from,
                                        to,
                                        null,
                                        PageRequest.of(0, 20, Sort.by("soldAt"))))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));

        verifyNoInteractions(sales);
    }

    @Test
    void listSales_withoutDates_usesDefaultWindowOfThirtyDays() {
        long userId = 13L;
        when(sales.searchForUser(eq(userId), any(), any(), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));
        Instant beforeCall = Instant.now();

        salesService.listSales(userId, null, null, null, PageRequest.of(0, 10));

        Instant afterCall = Instant.now();
        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(sales)
                .searchForUser(
                        eq(userId),
                        startCaptor.capture(),
                        endCaptor.capture(),
                        eq(null),
                        eq(null),
                        any(Pageable.class));

        Instant capturedStart = startCaptor.getValue();
        Instant capturedEnd = endCaptor.getValue();
        assertThat(capturedEnd).isBetween(beforeCall, afterCall);
        assertThat(capturedStart).isEqualTo(capturedEnd.minus(30, java.time.temporal.ChronoUnit.DAYS));
    }

    @Test
    void listSales_clampsRequestedPageSizeToMaxLimit() {
        long userId = 9L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        Pageable requested = PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "soldAt"));
        when(sales.searchForUser(eq(userId), eq(from), eq(to), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        salesService.listSales(userId, from, to, null, requested);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sales)
                .searchForUser(eq(userId), eq(from), eq(to), eq(null), eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(200);
    }

    @Test
    void listSales_keepsRequestedPageSizeWhenWithinLimit() {
        long userId = 10L;
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-28T23:59:59Z");
        Pageable requested = PageRequest.of(1, 25, Sort.by(Sort.Direction.DESC, "soldAt"));
        when(sales.searchForUser(eq(userId), eq(from), eq(to), eq(3L), eq(null), any(Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty(requested));

        salesService.listSales(userId, from, to, 3L, requested);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sales)
                .searchForUser(eq(userId), eq(from), eq(to), eq(3L), eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
    }
}
