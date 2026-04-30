package com.lojapp.service;

import com.lojapp.entity.InventoryBalance;
import com.lojapp.entity.InventoryMovement;
import com.lojapp.entity.InventoryMovementType;
import com.lojapp.entity.Product;
import com.lojapp.entity.User;
import com.lojapp.config.CacheNames;
import com.lojapp.dto.dashboard.InventoryKpiResponse;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.domain.inventory.ManualStockAdjustment;
import com.lojapp.domain.inventory.StockLedgerDelta;
import com.lojapp.dto.inventory.LowStockResponse;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.exception.domain.InsufficientStockException;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.ProductNotFoundException;
import com.lojapp.repository.InventoryBalanceRepository;
import com.lojapp.repository.InventoryMovementRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.application.idempotency.ApiIdempotencyService;
import com.lojapp.application.idempotency.RequestFingerprint;
import com.lojapp.service.contract.InventoryServiceContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService implements InventoryServiceContract {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    /** Linha agregada produto + saldo (evita N+1 em KPIs e stock baixo). */
    private record ProductStockRow(
            long productId, String name, BigDecimal minimumStock, BigDecimal quantity) {}

    private final ProductRepository products;
    private final UserRepository users;
    private final InventoryMovementRepository inventoryMovements;
    private final InventoryBalanceRepository inventoryBalances;
    private final AuditService auditService;
    private final ApiIdempotencyService idempotencyService;

    public InventoryService(
            ProductRepository products,
            UserRepository users,
            InventoryMovementRepository inventoryMovements,
            InventoryBalanceRepository inventoryBalances,
            AuditService auditService,
            ApiIdempotencyService idempotencyService) {
        this.products = products;
        this.users = users;
        this.inventoryMovements = inventoryMovements;
        this.inventoryBalances = inventoryBalances;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Regista movimento e atualiza saldo. Quantidade com sinal: entradas positivas, saídas (venda)
     * negativas.
     */
    private void registerStockMovement(
            User user,
            Product product,
            InventoryMovementType movementType,
            BigDecimal quantity,
            String source,
            Long sourceId) {
        InventoryMovement movement = new InventoryMovement();
        movement.setUser(user);
        movement.setProduct(product);
        movement.setMovementType(movementType.name());
        movement.setQuantity(quantity);
        movement.setSource(source);
        movement.setSourceId(sourceId);
        inventoryMovements.save(movement);

        InventoryBalance balance = loadOrCreateBalanceForUpdate(user, product);
        BigDecimal newQty = balance.getQuantity().add(quantity);
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw movementType == InventoryMovementType.ADJUSTMENT
                    ? new InsufficientStockException(
                            "O saldo não pode ficar negativo; reduza a saída ou aumente a entrada no ajuste.")
                    : new InsufficientStockException();
        }
        balance.setQuantity(newQty);
        balance.setUpdatedAt(Instant.now());
        inventoryBalances.save(balance);
    }

    /** Obtém ou cria a linha de saldo com lock pessimista (PESSIMISTIC_WRITE) para vendas/ajustes concorrentes. */
    private InventoryBalance loadOrCreateBalanceForUpdate(User user, Product product) {
        Long userId = user.getId();
        Long productId = product.getId();
        Optional<InventoryBalance> locked = inventoryBalances.lockByUserAndProduct(userId, productId);
        if (locked.isPresent()) {
            return locked.get();
        }
        InventoryBalance created = new InventoryBalance();
        created.setUser(user);
        created.setProduct(product);
        created.setQuantity(BigDecimal.ZERO);
        created.setUpdatedAt(Instant.now());
        try {
            return inventoryBalances.saveAndFlush(created);
        } catch (DataIntegrityViolationException ex) {
            return inventoryBalances
                    .lockByUserAndProduct(userId, productId)
                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "Colisão ao criar saldo; linha não encontrada após retry", ex));
        }
    }

    private static final String SOURCE_SALE_REGISTER = "SALE_REGISTER";
    private static final String SOURCE_SALE_CANCEL = "SALE_CANCEL";
    private static final String SOURCE_NFE_IMPORT = "NFE_IMPORT";

    /** Quantidade vendida (positiva); persiste movimento {@link InventoryMovementType#SALE} com delta negativo. */
    @Transactional
    @CacheEvict(
            cacheNames = {
                CacheNames.DASHBOARD_BRANDS,
                CacheNames.DASHBOARD_PRODUCT_ABC,
                CacheNames.DASHBOARD_INVENTORY_KPIS
            },
            allEntries = true)
    public void decreaseForSale(User user, Product product, BigDecimal quantitySold, long saleId) {
        StockLedgerDelta delta = StockLedgerDelta.forSaleDecrease(quantitySold);
        registerStockMovement(
                user,
                product,
                InventoryMovementType.SALE,
                delta.signedQuantity(),
                SOURCE_SALE_REGISTER,
                saleId);
    }

    @Transactional
    @CacheEvict(
            cacheNames = {
                CacheNames.DASHBOARD_BRANDS,
                CacheNames.DASHBOARD_PRODUCT_ABC,
                CacheNames.DASHBOARD_INVENTORY_KPIS
            },
            allEntries = true)
    public void restoreStockForCancelledSale(
            User user, Product product, BigDecimal quantitySold, long saleId) {
        StockLedgerDelta delta = StockLedgerDelta.forSaleCancellationRestore(quantitySold);
        registerStockMovement(
                user,
                product,
                InventoryMovementType.ADJUSTMENT,
                delta.signedQuantity(),
                SOURCE_SALE_CANCEL,
                saleId);
    }

    /** Quantidade recebida na NFe (positiva); persiste movimento {@link InventoryMovementType#ENTRY}. */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DASHBOARD_INVENTORY_KPIS, allEntries = true)
    public void increaseFromNfe(User user, Product product, BigDecimal quantity, long nfeEntryId) {
        StockLedgerDelta delta = StockLedgerDelta.forNfeEntry(quantity);
        registerStockMovement(
                user,
                product,
                InventoryMovementType.ENTRY,
                delta.signedQuantity(),
                SOURCE_NFE_IMPORT,
                nfeEntryId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableQuantity(long userId, long productId) {
        return inventoryBalances
                .findByUser_IdAndProduct_Id(userId, productId)
                .map(InventoryBalance::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    /** Saldo atual do produto do utilizador; 404 se o produto não existir ou for de outra loja. */
    @Transactional(readOnly = true)
    public BigDecimal getStockForOwnedProduct(long userId, long productId) {
        if (products.findByIdAndUser_Id(productId, userId).isEmpty()) {
            throw new ProductNotFoundException();
        }
        return getAvailableQuantity(userId, productId);
    }

    /**
     * Pré-checagem de saldo sem lock  pode divergir de uma venda concurrente; a garantia é {@link
     * #registerStockMovement} com lock pessimista.
     */
    public void assertSufficientStock(long userId, long productId, BigDecimal quantityRequested) {
        BigDecimal available = getAvailableQuantity(userId, productId);
        if (available.compareTo(quantityRequested) < 0) {
            throw new InsufficientStockException();
        }
    }

    public void adjustStock(
            long userId, StockAdjustmentRequest request, Optional<String> idempotencyKeyHeader) {
        String fingerprint = RequestFingerprint.stockAdjustRequestHash(request);
        idempotencyService.runStockAdjust(
                userId,
                idempotencyKeyHeader,
                fingerprint,
                () -> applyManualStockAdjustment(userId, request));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DASHBOARD_INVENTORY_KPIS, allEntries = true)
    public void applyManualStockAdjustment(long userId, StockAdjustmentRequest request) {
        ManualStockAdjustment adj = ManualStockAdjustment.fromRequest(request);
        StockLedgerDelta delta = StockLedgerDelta.forManualAdjustment(adj.quantity());
        Product product =
                products
                        .findByIdAndUser_Id(adj.productId(), userId)
                        .orElseThrow(ProductNotFoundException::new);
        registerStockMovement(
                users.getReferenceById(userId),
                product,
                InventoryMovementType.ADJUSTMENT,
                delta.signedQuantity(),
                adj.reason(),
                null);
        log.info(
                "Stock ajustado userId={} productId={} delta={}",
                userId,
                adj.productId(),
                adj.quantity());
        auditService.log(
                userId,
                "STOCK_ADJUST",
                "productId=%d delta=%s reason=%s"
                        .formatted(adj.productId(), adj.quantity(), adj.reason()));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.DASHBOARD_INVENTORY_KPIS, key = "#userId")
    public InventoryKpiResponse inventoryKpis(long userId) {
        ProductRepository.InventoryKpiProjection kpi = products.calcInventoryKpis(userId);
        int totalSkus = kpi.getTotalProducts() == null ? 0 : kpi.getTotalProducts().intValue();
        BigDecimal totalUnits = kpi.getTotalUnits() == null ? BigDecimal.ZERO : kpi.getTotalUnits();
        int lowStockCount = kpi.getLowStock() == null ? 0 : kpi.getLowStock().intValue();
        int withStock = kpi.getWithStock() == null ? 0 : kpi.getWithStock().intValue();
        return new InventoryKpiResponse(totalSkus, totalUnits, lowStockCount, withStock);
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> listLowStock(long userId) {
        List<ProductRepository.ProductStockRowProjection> rows = products.findLowStockRowsForUser(userId);
        List<LowStockResponse> low = new ArrayList<>(rows.size());
        for (ProductRepository.ProductStockRowProjection row : rows) {
            low.add(
                    new LowStockResponse(
                            row.getProductId(), row.getName(), row.getQuantity(), row.getMinimumStock()));
        }
        return low;
    }

    private List<ProductStockRow> loadProductStockRows(long userId) {
        List<ProductRepository.ProductStockRowProjection> raw = products.findProductStockRowsForUser(userId);
        List<ProductStockRow> out = new ArrayList<>(raw.size());
        for (ProductRepository.ProductStockRowProjection r : raw) {
            out.add(
                    new ProductStockRow(
                            r.getProductId(),
                            r.getName(),
                            r.getMinimumStock(),
                            r.getQuantity()));
        }
        return out;
    }
}
