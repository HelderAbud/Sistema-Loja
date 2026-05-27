package com.lojapp.controller;

import com.lojapp.dto.inventory.LowStockResponse;
import com.lojapp.dto.inventory.ProductStockResponse;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.security.JwtUser;
import com.lojapp.service.contract.InventoryServiceContract;
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
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Estoque")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class InventoryController {

    private final InventoryServiceContract inventory;

    public InventoryController(InventoryServiceContract inventory) {
        this.inventory = inventory;
    }

    @Operation(
            summary = "Ajustar stock manualmente",
            description =
                    "Delta em `quantity`: positivo entra stock, negativo sai. Motivo obrigatório. "
                            + "Opcionalmente enviar `Idempotency-Key` para evitar duplicar o mesmo ajuste em retries.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ajuste aplicado"),
        @ApiResponse(
                responseCode = "400",
                description = "Validação (ex.: quantity zero, reason vazio)"),
        @ApiResponse(responseCode = "404", description = "Produto inexistente ou de outro utilizador")
    })
    @PostMapping("/inventory/adjust")
    public void adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            @Parameter(
                            description =
                                    "Opcional. Evita duplicar o mesmo ajuste em retries (24h por defeito).")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey,
            @AuthenticationPrincipal JwtUser principal) {
        inventory.adjustStock(
                principal.userId(), request, Optional.ofNullable(idempotencyKey));
    }

    @Operation(summary = "Listar produtos com stock abaixo do mínimo")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de itens em baixo stock",
            content =
                    @Content(
                            array = @ArraySchema(schema = @Schema(implementation = LowStockResponse.class))))
    @GetMapping("/inventory/low-stock")
    public List<LowStockResponse> lowStock(@AuthenticationPrincipal JwtUser principal) {
        return inventory.listLowStock(principal.userId());
    }

    @Operation(summary = "Consultar quantidade em stock de um produto")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                content =
                        @Content(schema = @Schema(implementation = ProductStockResponse.class))),
        @ApiResponse(responseCode = "404", description = "Produto inexistente ou de outro utilizador")
    })
    @GetMapping("/inventory/products/{productId}/stock")
    public ProductStockResponse productStock(
            @PathVariable long productId, @AuthenticationPrincipal JwtUser principal) {
        return new ProductStockResponse(inventory.getStockForOwnedProduct(principal.userId(), productId));
    }
}
