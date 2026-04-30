package com.lojapp.controller;

import com.lojapp.dto.hierarchy.ProductCollectionRequest;
import com.lojapp.dto.hierarchy.ProductCollectionResponse;
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
@Tag(name = "LojApp - Colecoes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class ProductCollectionController {

    private final LojappHierarchyServiceContract hierarchy;

    public ProductCollectionController(LojappHierarchyServiceContract hierarchy) {
        this.hierarchy = hierarchy;
    }

    @GetMapping("/product-collections")
    @Operation(summary = "Listar colecoes de uma marca")
    public List<ProductCollectionResponse> listCollections(
            @Parameter(description = "Id da marca", required = true) @RequestParam long brandId,
            @AuthenticationPrincipal JwtUser principal) {
        return hierarchy.listCollections(principal.userId(), brandId);
    }

    @GetMapping("/product-collections/{id}")
    @Operation(summary = "Obter colecao por id")
    public ProductCollectionResponse getCollection(
            @Parameter(description = "Id da colecao") @PathVariable long id,
            @AuthenticationPrincipal JwtUser principal) {
        return hierarchy.getCollection(principal.userId(), id);
    }

    @PostMapping("/product-collections")
    @Operation(summary = "Criar colecao")
    public ProductCollectionResponse createCollection(
            @Valid @RequestBody ProductCollectionRequest request,
            @AuthenticationPrincipal JwtUser principal) {
        return hierarchy.createCollection(principal.userId(), request);
    }
}
