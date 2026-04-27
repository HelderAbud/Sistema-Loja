package com.lojapp.controller;

import com.lojapp.dto.brand.BrandRequest;
import com.lojapp.dto.brand.BrandResponse;
import com.lojapp.security.JwtUser;
import com.lojapp.service.LojappCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Marcas")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class BrandController {

    private final LojappCatalogService catalog;

    public BrandController(LojappCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/brands")
    public List<BrandResponse> listBrands(@AuthenticationPrincipal JwtUser principal) {
        return catalog.listBrands(principal.userId());
    }

    @PostMapping("/brands")
    public BrandResponse createBrand(
            @Valid @RequestBody BrandRequest request, @AuthenticationPrincipal JwtUser principal) {
        return catalog.createBrand(principal.userId(), request);
    }

    @Operation(summary = "Atualizar marca", description = "O nome deve ser unico por loja (comparacao sem distinguir maiusculas).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Marca atualizada"),
        @ApiResponse(responseCode = "404", description = "Marca inexistente ou de outra loja"),
        @ApiResponse(responseCode = "409", description = "Ja existe outra marca com o mesmo nome")
    })
    @PutMapping("/brands/{id}")
    public BrandResponse updateBrand(
            @Parameter(description = "Id da marca") @PathVariable long id,
            @Valid @RequestBody BrandRequest request,
            @AuthenticationPrincipal JwtUser principal) {
        return catalog.updateBrand(principal.userId(), id, request);
    }

    @Operation(
            summary = "Eliminar marca",
            description =
                    "Produtos que usavam esta marca ficam sem marca (equivalente a marca nao informada no catalogo).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Marca eliminada"),
        @ApiResponse(responseCode = "404", description = "Marca inexistente ou de outra loja")
    })
    @DeleteMapping("/brands/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(
            @Parameter(description = "Id da marca") @PathVariable long id,
            @AuthenticationPrincipal JwtUser principal) {
        catalog.deleteBrand(principal.userId(), id);
    }
}
