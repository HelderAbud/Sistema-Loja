package com.lojapp.controller;

import com.lojapp.dto.product.ProductPageResponse;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.dto.product.ProductResponse;
import com.lojapp.security.JwtUser;
import com.lojapp.service.LojappCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Produtos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class ProductController {

    private final LojappCatalogService catalog;

    public ProductController(LojappCatalogService catalog) {
        this.catalog = catalog;
    }

    @Operation(
            summary = "Listar produtos (paginado)",
            description =
                    "A resposta e um **objeto** com a lista em `content` e metadados (`totalElements`, "
                            + "`totalPages`, `size`, `number`, `first`, `last`). Nao e um array na raiz do JSON.\n\n"
                            + "**Paginacao (Spring Data):** `page` (base 0), `size` (omissao 20; maximo 200 no "
                            + "servidor), `sort` (ex.: `name,asc` ou `name,desc`).\n\n"
                            + "**Filtros opcionais:** `brandId` - so produtos dessa marca; `q` - substring do nome "
                            + "(comparacao case-insensitive); `lowStock=true` - stock abaixo do minimo ou sem "
                            + "registo de saldo quando o minimo e maior que zero.")
    @ApiResponse(
            responseCode = "200",
            description = "Pagina de produtos",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductPageResponse.class)))
    @GetMapping("/products")
    public ProductPageResponse listProducts(
            @ParameterObject
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable,
            @Parameter(description = "Filtrar por id da marca do utilizador atual", example = "1")
            @RequestParam(required = false)
            Long brandId,
            @Parameter(description = "Texto a procurar no nome do produto (contem)", example = "caderno")
            @RequestParam(required = false)
            String q,
            @Parameter(
                    description =
                            "Se `true`, so produtos com stock baixo (quantidade &lt; minimo, ou sem saldo com "
                                    + "minimo &gt; 0)")
            @RequestParam(required = false)
            Boolean lowStock,
            @AuthenticationPrincipal JwtUser principal) {
        boolean low = Boolean.TRUE.equals(lowStock);
        return ProductPageResponse.from(
                catalog.searchProducts(principal.userId(), brandId, q, low, pageable));
    }

    @Operation(summary = "Criar produto")
    @ApiResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductResponse.class)))
    @PostMapping("/products")
    public ProductResponse createProduct(
            @Valid @RequestBody ProductRequest request, @AuthenticationPrincipal JwtUser principal) {
        return catalog.createProduct(principal.userId(), request);
    }

    @Operation(summary = "Atualizar produto")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Produto inexistente ou de outro utilizador")
    })
    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(
            @Parameter(description = "Id do produto", example = "10") @PathVariable long id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal JwtUser principal) {
        return catalog.updateProduct(principal.userId(), id, request);
    }
}
