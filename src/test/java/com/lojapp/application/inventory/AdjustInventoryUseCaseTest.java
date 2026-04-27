package com.lojapp.application.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.lojapp.application.idempotency.ApiIdempotencyService;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.service.contract.InventoryServiceContract;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdjustInventoryUseCaseTest {

    @Mock private ApiIdempotencyService idempotencyService;
    @Mock private InventoryServiceContract inventoryService;

    private AdjustInventoryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AdjustInventoryUseCase(idempotencyService, inventoryService);
        doAnswer(
                        inv -> {
                            inv.getArgument(3, Runnable.class).run();
                            return null;
                        })
                .when(idempotencyService)
                .runStockAdjust(anyLong(), any(), anyString(), any(Runnable.class));
    }

    @Test
    void execute_invokesManualAdjustmentInsideIdempotencyShell() {
        long userId = 1L;
        var request = new StockAdjustmentRequest(9L, new BigDecimal("2"), "motivo");
        Optional<String> key = Optional.of("k1");

        useCase.execute(userId, request, key);

        verify(idempotencyService).runStockAdjust(eq(userId), eq(key), anyString(), any(Runnable.class));
        verify(inventoryService).applyManualStockAdjustment(userId, request);
    }
}
