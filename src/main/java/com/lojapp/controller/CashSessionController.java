package com.lojapp.controller;

import com.lojapp.application.contract.CloseCashSessionUseCaseContract;
import com.lojapp.application.contract.GetCashSessionClosePreviewUseCaseContract;
import com.lojapp.application.contract.GetCurrentCashSessionUseCaseContract;
import com.lojapp.application.contract.OpenCashSessionUseCaseContract;
import com.lojapp.dto.cash.CloseCashSessionRequest;
import com.lojapp.dto.cash.CloseCashSessionPreviewResponse;
import com.lojapp.dto.cash.CloseCashSessionResponse;
import com.lojapp.dto.cash.CurrentCashSessionResponse;
import com.lojapp.dto.cash.OpenCashSessionRequest;
import com.lojapp.dto.cash.OpenCashSessionResponse;
import com.lojapp.security.JwtUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp/pos/cash-sessions")
@Tag(name = "LojApp - PDV Caixa")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CASHIER','MANAGER')")
public class CashSessionController {

    private final OpenCashSessionUseCaseContract openCashSessionUseCase;
    private final CloseCashSessionUseCaseContract closeCashSessionUseCase;
    private final GetCurrentCashSessionUseCaseContract getCurrentCashSessionUseCase;
    private final GetCashSessionClosePreviewUseCaseContract getCashSessionClosePreviewUseCase;

    public CashSessionController(
            OpenCashSessionUseCaseContract openCashSessionUseCase,
            CloseCashSessionUseCaseContract closeCashSessionUseCase,
            GetCurrentCashSessionUseCaseContract getCurrentCashSessionUseCase,
            GetCashSessionClosePreviewUseCaseContract getCashSessionClosePreviewUseCase) {
        this.openCashSessionUseCase = openCashSessionUseCase;
        this.closeCashSessionUseCase = closeCashSessionUseCase;
        this.getCurrentCashSessionUseCase = getCurrentCashSessionUseCase;
        this.getCashSessionClosePreviewUseCase = getCashSessionClosePreviewUseCase;
    }

    @GetMapping("/current")
    public CurrentCashSessionResponse currentCashSession(@AuthenticationPrincipal JwtUser principal) {
        return getCurrentCashSessionUseCase.execute(principal.userId());
    }

    @GetMapping("/{id}/close-preview")
    public CloseCashSessionPreviewResponse closePreview(
            @PathVariable("id") long cashSessionId,
            @RequestParam(value = "countedAmount", required = false) BigDecimal countedAmount,
            @AuthenticationPrincipal JwtUser principal) {
        return getCashSessionClosePreviewUseCase.execute(
                principal.userId(), cashSessionId, countedAmount);
    }

    @PostMapping("/open")
    public OpenCashSessionResponse openCashSession(
            @Valid @RequestBody OpenCashSessionRequest request,
            @AuthenticationPrincipal JwtUser principal) {
        return openCashSessionUseCase.execute(principal.userId(), principal.userId(), request);
    }

    @PostMapping("/close")
    public CloseCashSessionResponse closeCashSession(
            @Valid @RequestBody CloseCashSessionRequest request,
            @AuthenticationPrincipal JwtUser principal) {
        return closeCashSessionUseCase.execute(principal.userId(), principal.userId(), request);
    }
}
