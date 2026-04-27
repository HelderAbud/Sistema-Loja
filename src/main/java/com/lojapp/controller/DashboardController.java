package com.lojapp.controller;

import com.lojapp.dto.dashboard.BrandDashboardResponse;
import com.lojapp.dto.dashboard.InventoryKpiResponse;
import com.lojapp.dto.dashboard.ProductAbcResponse;
import com.lojapp.security.JwtUser;
import com.lojapp.service.DashboardService;
import com.lojapp.service.contract.InventoryServiceContract;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Dashboard")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class DashboardController {

    private final DashboardService dashboard;
    private final InventoryServiceContract inventory;

    public DashboardController(DashboardService dashboard, InventoryServiceContract inventory) {
        this.dashboard = dashboard;
        this.inventory = inventory;
    }

    @GetMapping("/dashboard/brands")
    public BrandDashboardResponse dashboardByBrand(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false, defaultValue = "50") @Min(1) @Max(200) int brandLimit,
            @RequestParam(required = false, defaultValue = "0") @Min(0) int brandOffset,
            @AuthenticationPrincipal JwtUser principal) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(30, ChronoUnit.DAYS) : from;
        return dashboard.brandDashboard(principal.userId(), start, end, brandLimit, brandOffset);
    }

    @GetMapping("/dashboard/products-abc")
    public ProductAbcResponse dashboardProductAbc(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @AuthenticationPrincipal JwtUser principal) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(30, ChronoUnit.DAYS) : from;
        return dashboard.productAbc(principal.userId(), start, end);
    }

    @GetMapping("/dashboard/inventory-kpis")
    public InventoryKpiResponse dashboardInventoryKpis(@AuthenticationPrincipal JwtUser principal) {
        return inventory.inventoryKpis(principal.userId());
    }
}
