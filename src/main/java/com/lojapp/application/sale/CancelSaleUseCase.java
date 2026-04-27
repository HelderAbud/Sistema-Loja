package com.lojapp.application.sale;

import com.lojapp.domain.sale.SalePendingCancellation;
import com.lojapp.entity.Sale;
import com.lojapp.exception.domain.SaleNotFoundException;
import com.lojapp.repository.SaleRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.contract.InventoryServiceContract;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelSaleUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelSaleUseCase.class);

    private final SaleRepository sales;
    private final InventoryServiceContract inventoryService;
    private final AuditService auditService;

    public CancelSaleUseCase(
            SaleRepository sales,
            InventoryServiceContract inventoryService,
            AuditService auditService) {
        this.sales = sales;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
    }

    @Transactional
    public void execute(long userId, long saleId) {
        Sale sale =
                sales.findByIdAndUser_Id(saleId, userId).orElseThrow(SaleNotFoundException::new);
        SalePendingCancellation pending =
                SalePendingCancellation.fromPersistedState(
                        sale.getId(), sale.getCancelledAt(), sale.getQuantity());
        sale.setCancelledAt(Instant.now());
        inventoryService.restoreStockForCancelledSale(
                sale.getUser(),
                sale.getProduct(),
                pending.quantityToRestore(),
                sale.getId());
        log.info("Venda cancelada userId={} saleId={}", userId, saleId);
        auditService.log(
                userId,
                "SALE_CANCELLED",
                "saleId=%d productId=%d qty=%s"
                        .formatted(sale.getId(), sale.getProduct().getId(), sale.getQuantity()));
    }
}
