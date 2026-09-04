package com.lojapp.application.usecases.sale;

import com.lojapp.domain.sale.SalePendingCancellation;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SaleItem;
import com.lojapp.exception.domain.SaleAlreadyCancelledException;
import com.lojapp.exception.domain.SaleCashSessionAlreadyClosedException;
import com.lojapp.exception.domain.SaleNotFoundException;
import com.lojapp.repository.CommissionAccrualRepository;
import com.lojapp.repository.SaleItemRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.contract.InventoryServiceContract;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelSaleUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelSaleUseCase.class);

    private final SaleRepository sales;
    private final SaleItemRepository saleItems;
    private final CommissionAccrualRepository commissionAccruals;
    private final InventoryServiceContract inventoryService;
    private final AuditService auditService;

    public CancelSaleUseCase(
            SaleRepository sales,
            SaleItemRepository saleItems,
            CommissionAccrualRepository commissionAccruals,
            InventoryServiceContract inventoryService,
            AuditService auditService) {
        this.sales = sales;
        this.saleItems = saleItems;
        this.commissionAccruals = commissionAccruals;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
    }

    @Transactional
    public void execute(long userId, long saleId) {
        Sale sale =
                sales.findByIdAndUser_Id(saleId, userId).orElseThrow(SaleNotFoundException::new);
        if (sale.getCancelledAt() != null) {
            throw new SaleAlreadyCancelledException();
        }
        refuseIfCashSessionClosed(sale);
        List<SaleItem> items = saleItems.findBySale_IdAndUser_Id(sale.getId(), userId);
        if (items.isEmpty()) {
            SalePendingCancellation pending =
                    SalePendingCancellation.fromPersistedState(
                            sale.getId(), sale.getCancelledAt(), sale.getQuantity());
            inventoryService.restoreStockForCancelledSale(
                    sale.getUser(),
                    sale.getProduct(),
                    pending.quantityToRestore(),
                    sale.getId());
        } else {
            for (SaleItem item : items) {
                inventoryService.restoreStockForCancelledSale(
                        sale.getUser(), item.getProduct(), item.getQuantity(), sale.getId());
            }
        }
        commissionAccruals.deleteBySale_IdAndUser_Id(sale.getId(), userId);
        sale.setCancelledAt(Instant.now());
        log.info("Venda cancelada userId={} saleId={} lines={}", userId, saleId, Math.max(items.size(), 1));
        auditService.log(
                userId,
                "SALE_CANCELLED",
                "saleId=%d lines=%d"
                        .formatted(sale.getId(), Math.max(items.size(), 1)));
    }

    private static void refuseIfCashSessionClosed(Sale sale) {
        CashSession session = sale.getCashSession();
        if (session != null && session.getStatus() == CashSessionStatus.CLOSED) {
            throw new SaleCashSessionAlreadyClosedException();
        }
    }
}
