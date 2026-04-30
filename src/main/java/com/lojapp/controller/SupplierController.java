package com.lojapp.controller;

import com.lojapp.dto.supplier.SupplierRequest;
import com.lojapp.dto.supplier.SupplierResponse;
import com.lojapp.security.JwtUser;
import com.lojapp.service.contract.LojappHierarchyServiceContract;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Fornecedores")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class SupplierController {

    private final LojappHierarchyServiceContract hierarchy;

    public SupplierController(LojappHierarchyServiceContract hierarchy) {
        this.hierarchy = hierarchy;
    }

    @GetMapping("/suppliers")
    @Operation(summary = "Listar fornecedores da loja")
    public List<SupplierResponse> listSuppliers(@AuthenticationPrincipal JwtUser principal) {
        return hierarchy.listSuppliers(principal.userId());
    }

    @GetMapping("/suppliers/{id}")
    @Operation(summary = "Obter fornecedor por id")
    public SupplierResponse getSupplier(
            @Parameter(description = "Id do fornecedor") @PathVariable long id,
            @AuthenticationPrincipal JwtUser principal) {
        return hierarchy.getSupplier(principal.userId(), id);
    }

    @PostMapping("/suppliers")
    @Operation(summary = "Criar fornecedor", description = "CNPJ/CPF opcional; ºnico por loja quando informado.")
    public SupplierResponse createSupplier(
            @Valid @RequestBody SupplierRequest request, @AuthenticationPrincipal JwtUser principal) {
        return hierarchy.createSupplier(principal.userId(), request);
    }
}
