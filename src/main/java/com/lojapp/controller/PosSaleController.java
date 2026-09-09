package com.lojapp.controller;

import com.lojapp.application.contract.ConfirmPosSalePaymentUseCaseContract;
import com.lojapp.application.contract.CreatePosSaleUseCaseContract;
import com.lojapp.application.contract.ListPendingPosPaymentsUseCaseContract;
import com.lojapp.dto.sale.PosSaleFinalizeRequest;
import com.lojapp.dto.sale.PosSaleFinalizeResponse;
import com.lojapp.dto.sale.PosSalePaymentView;
import com.lojapp.security.JwtUser;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp/pos/sales")
@Tag(name = "LojApp - PDV Vendas")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CASHIER','SELLER','MANAGER','USER','ADMIN')")
public class PosSaleController {

    private final CreatePosSaleUseCaseContract createPosSaleUseCase;
    private final ListPendingPosPaymentsUseCaseContract listPendingPosPaymentsUseCase;
    private final ConfirmPosSalePaymentUseCaseContract confirmPosSalePaymentUseCase;

    public PosSaleController(
            CreatePosSaleUseCaseContract createPosSaleUseCase,
            ListPendingPosPaymentsUseCaseContract listPendingPosPaymentsUseCase,
            ConfirmPosSalePaymentUseCaseContract confirmPosSalePaymentUseCase) {
        this.createPosSaleUseCase = createPosSaleUseCase;
        this.listPendingPosPaymentsUseCase = listPendingPosPaymentsUseCase;
        this.confirmPosSalePaymentUseCase = confirmPosSalePaymentUseCase;
    }

    @GetMapping("/pending-payments")
    public List<PosSalePaymentView> pendingPayments(@AuthenticationPrincipal JwtUser principal) {
        return listPendingPosPaymentsUseCase.execute(principal.userId());
    }

    @PostMapping("/{saleId}/payments/{paymentId}/confirm")
    public PosSalePaymentView confirmPayment(
            @PathVariable("saleId") long saleId,
            @PathVariable("paymentId") long paymentId,
            @AuthenticationPrincipal JwtUser principal) {
        return confirmPosSalePaymentUseCase.execute(principal.userId(), saleId, paymentId);
    }

    @PostMapping("/finalize")
    public PosSaleFinalizeResponse finalizeSale(
            @Valid @RequestBody PosSaleFinalizeRequest request,
            @Parameter(
                            description =
                                    "Obrigatória no PDV. Repetição com mesma chave/corpo devolve a mesma venda finalizada.")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey,
            @AuthenticationPrincipal JwtUser principal) {
        return createPosSaleUseCase.execute(
                principal.userId(), request, Optional.ofNullable(idempotencyKey));
    }
}
