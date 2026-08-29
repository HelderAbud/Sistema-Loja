package com.lojapp.controller;

import com.lojapp.dto.commission.CommissionAccrualResponse;
import com.lojapp.security.JwtUser;
import com.lojapp.service.contract.CommissionReportServiceContract;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Relatorio de comissoes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
public class CommissionAccrualController {

    private final CommissionReportServiceContract report;

    public CommissionAccrualController(CommissionReportServiceContract report) {
        this.report = report;
    }

    @GetMapping("/commission-accruals")
    @Operation(summary = "Listar lancamentos de comissao no intervalo (padrao: 30 dias)")
    public List<CommissionAccrualResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @AuthenticationPrincipal JwtUser principal) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(30, ChronoUnit.DAYS) : from;
        return report.list(principal.userId(), start, end);
    }

    @GetMapping(value = "/commission-accruals.csv", produces = "text/csv")
    @Operation(summary = "Exportar lancamentos de comissao em CSV")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @AuthenticationPrincipal JwtUser principal) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(30, ChronoUnit.DAYS) : from;
        String csv = report.toCsv(principal.userId(), start, end);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"commission-accruals.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
