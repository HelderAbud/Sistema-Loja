package com.lojapp.controller;

import com.lojapp.dto.seller.SellerRequest;
import com.lojapp.dto.seller.SellerResponse;
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
@Tag(name = "LojApp - Vendedoras")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','CASHIER','SELLER','MANAGER')")
public class SellerController {

    private final CommissionCatalogServiceContract catalog;

    public SellerController(CommissionCatalogServiceContract catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/sellers")
    @Operation(summary = "Listar vendedoras da loja")
    public List<SellerResponse> listSellers(@AuthenticationPrincipal JwtUser principal) {
        return catalog.listSellers(principal.userId());
    }

    @PostMapping("/sellers")
    @Operation(summary = "Criar vendedora")
    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
    public SellerResponse createSeller(
            @Valid @RequestBody SellerRequest request, @AuthenticationPrincipal JwtUser principal) {
        return catalog.createSeller(principal.userId(), request);
    }
}
