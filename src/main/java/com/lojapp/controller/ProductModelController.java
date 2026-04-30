package com.lojapp.controller;

import com.lojapp.dto.hierarchy.ProductModelRequest;
import com.lojapp.dto.hierarchy.ProductModelResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Modelos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class ProductModelController {

    private final LojappHierarchyServiceContract hierarchy;

    public ProductModelController(LojappHierarchyServiceContract hierarchy) {
        this.hierarchy = hierarchy;
    }

    @GetMapping("/product-models")
    @Operation(summary = "Listar modelos de uma marca", description = "Filtro opcional por colecao.")
    public List<ProductModelResponse> listModels(
            @Parameter(description = "Id da marca", required = true) @RequestParam long brandId,
            @Parameter(description = "Id da colecao (opcional)") @RequestParam(required = false)
                    Long collectionId,
            @AuthenticationPrincipal JwtUser principal) {
        return hierarchy.listModels(principal.userId(), brandId, collectionId);
    }

    @GetMapping("/product-models/{id}")
    @Operation(summary = "Obter modelo por id")
    public ProductModelResponse getModel(
            @Parameter(description = "Id do modelo") @PathVariable long id,
            @AuthenticationPrincipal JwtUser principal) {
        return hierarchy.getModel(principal.userId(), id);
    }

    @PostMapping("/product-models")
    @Operation(summary = "Criar modelo")
    public ProductModelResponse createModel(
            @Valid @RequestBody ProductModelRequest request,
            @AuthenticationPrincipal JwtUser principal) {
        return hierarchy.createModel(principal.userId(), request);
    }
}
