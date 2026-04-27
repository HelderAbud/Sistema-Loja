package com.lojapp.controller;

import com.lojapp.dto.sale.SalePageResponse;
import com.lojapp.dto.sale.SaleCreatedResponse;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.dto.sale.SalesDailyPointResponse;
import com.lojapp.dto.sale.SalesSummaryResponse;
import com.lojapp.security.JwtUser;
import com.lojapp.service.contract.SalesServiceContract;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Vendas")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class SaleController {

    private final SalesServiceContract sales;

    public SaleController(SalesServiceContract sales) {
        this.sales = sales;
    }

    @GetMapping("/sales")
    public SalePageResponse listSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long brandId,
            @PageableDefault(size = 20, sort = "soldAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            @AuthenticationPrincipal JwtUser principal) {
        return sales.listSales(principal.userId(), from, to, productId, brandId, pageable);
    }

    @GetMapping("/sales/summary")
    public SalesSummaryResponse summarizeSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long brandId,
            @AuthenticationPrincipal JwtUser principal) {
        return sales.summarizeSales(principal.userId(), from, to, productId, brandId);
    }

    @GetMapping("/sales/daily")
    public List<SalesDailyPointResponse> summarizeSalesDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long brandId,
            @AuthenticationPrincipal JwtUser principal) {
        return sales.summarizeSalesDaily(principal.userId(), from, to, productId, brandId);
    }

    @PostMapping("/sales/{id}/cancel")
    public void cancelSale(
            @PathVariable("id") long saleId, @AuthenticationPrincipal JwtUser principal) {
        sales.cancelSale(principal.userId(), saleId);
    }

    @PostMapping("/sales")
    public SaleCreatedResponse registerSale(
            @Valid @RequestBody SaleRequest request,
            @Parameter(
                            description =
                                    "Opcional. Replay com a mesma chave e o mesmo corpo devolve a mesma venda (24h por defeito).")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey,
            @AuthenticationPrincipal JwtUser principal) {
        return sales.registerSale(
                principal.userId(), request, Optional.ofNullable(idempotencyKey));
    }
}
