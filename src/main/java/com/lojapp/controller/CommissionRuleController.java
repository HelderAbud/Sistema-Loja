package com.lojapp.controller;

import com.lojapp.dto.commission.CommissionRuleRequest;
import com.lojapp.dto.commission.CommissionRuleResponse;
import com.lojapp.security.JwtUser;
import com.lojapp.service.contract.CommissionCatalogServiceContract;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Regras de comissao")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
public class CommissionRuleController {

    private final CommissionCatalogServiceContract catalog;

    public CommissionRuleController(CommissionCatalogServiceContract catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/commission-rules")
    @Operation(summary = "Listar regras de comissao (percentual vigente a partir de validFrom)")
    public List<CommissionRuleResponse> listRules(@AuthenticationPrincipal JwtUser principal) {
        return catalog.listRules(principal.userId());
    }

    @PostMapping("/commission-rules")
    @Operation(
            summary = "Criar regra de comissao",
            description =
                    "brandId nulo = percentual global da loja. validFrom no futuro nao se aplica a vendas anteriores.")
    public CommissionRuleResponse createRule(
            @Valid @RequestBody CommissionRuleRequest request,
            @AuthenticationPrincipal JwtUser principal) {
        return catalog.createRule(principal.userId(), request);
    }
}
