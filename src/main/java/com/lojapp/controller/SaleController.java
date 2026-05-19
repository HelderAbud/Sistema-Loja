package com.lojapp.controller;

import com.lojapp.dto.sale.SalePageResponse;
import com.lojapp.dto.sale.SaleCreatedResponse;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.dto.sale.SalesDailyPointResponse;
import com.lojapp.dto.sale.SalesSummaryResponse;
import com.lojapp.security.JwtUser;
import com.lojapp.service.contract.SalesServiceContract;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Vendas")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class SaleController {

    private final SalesServiceContract sales;

    public SaleController(SalesServiceContract sales) {
        this.sales = sales;
    }

    @Operation(
            summary = "Listar vendas (paginado)",
            description =
                    "Resposta em envelope com `content` e metadados de página (Spring Data). "
                            + "Ordenação por defeito: `soldAt` descendente.")
    @ApiResponse(
            responseCode = "200",
            content =
                    @Content(schema = @Schema(implementation = SalePageResponse.class)))
    @GetMapping("/sales")
    public SalePageResponse listSales(
            @Parameter(description = "Início do intervalo (ISO-8601)", example = "2026-04-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @Parameter(description = "Fim do intervalo (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @Parameter(description = "Filtrar por produto") @RequestParam(required = false) Long productId,
            @Parameter(description = "Filtrar por marca") @RequestParam(required = false) Long brandId,
            @PageableDefault(size = 20, sort = "soldAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            @AuthenticationPrincipal JwtUser principal) {
        return sales.listSales(principal.userId(), from, to, productId, brandId, pageable);
    }

    @Operation(summary = "Resumo agregado de vendas no intervalo")
    @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = SalesSummaryResponse.class)))
    @GetMapping("/sales/summary")
    public SalesSummaryResponse summarizeSales(
            @Parameter(description = "Início (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @Parameter(description = "Fim (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @Parameter(description = "Filtrar por produto") @RequestParam(required = false) Long productId,
            @Parameter(description = "Filtrar por marca") @RequestParam(required = false) Long brandId,
            @AuthenticationPrincipal JwtUser principal) {
        return sales.summarizeSales(principal.userId(), from, to, productId, brandId);
    }

    @Operation(summary = "Vendas agregadas por dia (série temporal)")
    @ApiResponse(
            responseCode = "200",
            content =
                    @Content(
                            array = @ArraySchema(schema = @Schema(implementation = SalesDailyPointResponse.class))))
    @GetMapping("/sales/daily")
    public List<SalesDailyPointResponse> summarizeSalesDaily(
            @Parameter(description = "Início (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @Parameter(description = "Fim (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @Parameter(description = "Filtrar por produto") @RequestParam(required = false) Long productId,
            @Parameter(description = "Filtrar por marca") @RequestParam(required = false) Long brandId,
            @AuthenticationPrincipal JwtUser principal) {
        return sales.summarizeSalesDaily(principal.userId(), from, to, productId, brandId);
    }

    @Operation(summary = "Cancelar venda")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venda cancelada"),
        @ApiResponse(responseCode = "404", description = "Venda inexistente ou de outro utilizador")
    })
    @PostMapping("/sales/{id}/cancel")
    public void cancelSale(
            @Parameter(description = "Id da venda") @PathVariable("id") long saleId,
            @AuthenticationPrincipal JwtUser principal) {
        sales.cancelSale(principal.userId(), saleId);
    }

    @Operation(
            summary = "Registar venda",
            description = "Regista uma linha de venda e movimenta stock. Ver `SaleRequest` para custo opcional.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                content = @Content(schema = @Schema(implementation = SaleCreatedResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validação"),
        @ApiResponse(responseCode = "404", description = "Produto inexistente ou de outro utilizador"),
        @ApiResponse(responseCode = "409", description = "Conflito de negócio (ex.: stock insuficiente)")
    })
    @PostMapping("/sales")
    public SaleCreatedResponse registerSale(
            @Valid @RequestBody SaleRequest request,
            @Parameter(
                            description =
                                    "Opcional. Replay com a mesma chave e o mesmo corpo devolve a mesma venda (24h por defeito).")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey,
            @AuthenticationPrincipal JwtUser principal) {
        return sales.registerSale(
                principal.userId(), request, Optional.ofNullable(idempotencyKey));
    }
}
