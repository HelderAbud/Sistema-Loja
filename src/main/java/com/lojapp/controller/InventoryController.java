package com.lojapp.controller;

import com.lojapp.dto.inventory.LowStockResponse;
import com.lojapp.dto.inventory.ProductStockResponse;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.security.JwtUser;
import com.lojapp.service.contract.InventoryServiceContract;
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
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - Estoque")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class InventoryController {

    private final InventoryServiceContract inventory;

    public InventoryController(InventoryServiceContract inventory) {
        this.inventory = inventory;
    }

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

    @GetMapping("/inventory/low-stock")
    public List<LowStockResponse> lowStock(@AuthenticationPrincipal JwtUser principal) {
        return inventory.listLowStock(principal.userId());
    }

    @GetMapping("/inventory/products/{productId}/stock")
    public ProductStockResponse productStock(
            @PathVariable long productId, @AuthenticationPrincipal JwtUser principal) {
        return new ProductStockResponse(inventory.getStockForOwnedProduct(principal.userId(), productId));
    }
}
