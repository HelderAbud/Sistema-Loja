package com.lojapp.application.sale;

import com.lojapp.application.idempotency.ApiIdempotencyService;
import com.lojapp.application.idempotency.RequestFingerprint;
import com.lojapp.observability.LojappBusinessMetrics;
import com.lojapp.dto.sale.SaleCreatedResponse;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.domain.sale.SaleRegistrationLine;
import com.lojapp.entity.Product;
import com.lojapp.entity.Sale;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.ProductNotFoundException;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.contract.InventoryServiceContract;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Caso de uso: registar venda e baixar stock na mesma transacção. Suporta {@code Idempotency-Key}.
 */
@Service
public class CreateSaleUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateSaleUseCase.class);

    private final UserRepository users;
    private final ProductRepository products;
    private final SaleRepository sales;
    private final InventoryServiceContract inventoryService;
    private final AuditService auditService;
    private final ApiIdempotencyService idempotencyService;
    private final LojappBusinessMetrics businessMetrics;

    public CreateSaleUseCase(
            UserRepository users,
            ProductRepository products,
            SaleRepository sales,
            InventoryServiceContract inventoryService,
            AuditService auditService,
            ApiIdempotencyService idempotencyService,
            LojappBusinessMetrics businessMetrics) {
        this.users = users;
        this.products = products;
        this.sales = sales;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.businessMetrics = businessMetrics;
    }

    public SaleCreatedResponse execute(
            long userId, SaleRequest request, Optional<String> idempotencyKeyHeader) {
        String fingerprint = RequestFingerprint.saleRequestHash(request);
        return idempotencyService.runSaleCreate(
                userId, idempotencyKeyHeader, fingerprint, () -> persistSale(userId, request));
    }

    private SaleCreatedResponse persistSale(long userId, SaleRequest request) {
        User user = users.getReferenceById(userId);
        Product product =
                products
                        .findByIdAndUser_Id(request.productId(), userId)
                        .orElseThrow(ProductNotFoundException::new);

        SaleRegistrationLine line = SaleRegistrationLine.fromRequest(request, product.getCostPrice());

        Sale sale = new Sale();
        sale.setUser(user);
        sale.setProduct(product);
        sale.setQuantity(line.quantity());
        sale.setUnitPrice(line.unitPrice());
        sale.setUnitCost(line.unitCost());
        sales.save(sale);
        inventoryService.decreaseForSale(user, product, line.quantity(), sale.getId());
        log.info(
                "Venda registada userId={} saleId={} productId={} qty={}",
                userId,
                sale.getId(),
                product.getId(),
                line.quantity());
        businessMetrics.recordSaleRegistered();
        auditService.log(
                userId,
                "SALE_CREATED",
                "saleId=%d productId=%d qty=%s"
                        .formatted(sale.getId(), product.getId(), line.quantity()));
        return new SaleCreatedResponse(
                sale.getId(),
                product.getId(),
                sale.getQuantity(),
                sale.getUnitPrice(),
                sale.getUnitCost(),
                sale.getSoldAt());
    }
}
