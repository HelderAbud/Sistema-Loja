package com.lojapp.application.sale;

import com.lojapp.application.contract.CreatePosSaleUseCaseContract;
import com.lojapp.application.contract.PosSaleCommissionServiceContract;
import com.lojapp.application.idempotency.ApiIdempotencyService;
import com.lojapp.application.idempotency.RequestFingerprint;
import com.lojapp.domain.sale.SaleRegistrationLine;
import com.lojapp.dto.sale.PosSaleFinalizeRequest;
import com.lojapp.dto.sale.PosSaleFinalizeResponse;
import com.lojapp.dto.sale.PosSaleLineRequest;
import com.lojapp.dto.sale.PosSalePaymentRequest;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.Product;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SaleItem;
import com.lojapp.entity.SalePayment;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.CashSessionNotFoundException;
import com.lojapp.exception.domain.CashSessionNotOpenException;
import com.lojapp.exception.domain.PosSalePaymentTotalMismatchException;
import com.lojapp.exception.domain.ProductNotFoundException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.SaleItemRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.contract.InventoryServiceContract;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePosSaleUseCase implements CreatePosSaleUseCaseContract {

    private final UserRepository users;
    private final ProductRepository products;
    private final SaleRepository sales;
    private final SaleItemRepository saleItems;
    private final SalePaymentRepository salePayments;
    private final CashSessionRepository cashSessions;
    private final InventoryServiceContract inventoryService;
    private final AuditService auditService;
    private final ApiIdempotencyService idempotencyService;
    private final PosSaleCommissionServiceContract posSaleCommissionService;

    public CreatePosSaleUseCase(
            UserRepository users,
            ProductRepository products,
            SaleRepository sales,
            SaleItemRepository saleItems,
            SalePaymentRepository salePayments,
            CashSessionRepository cashSessions,
            InventoryServiceContract inventoryService,
            AuditService auditService,
            ApiIdempotencyService idempotencyService,
            PosSaleCommissionServiceContract posSaleCommissionService) {
        this.users = users;
        this.products = products;
        this.sales = sales;
        this.saleItems = saleItems;
        this.salePayments = salePayments;
        this.cashSessions = cashSessions;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.posSaleCommissionService = posSaleCommissionService;
    }

    @Transactional
    public PosSaleFinalizeResponse execute(
            long userId, PosSaleFinalizeRequest request, Optional<String> idempotencyKeyHeader) {
        String fingerprint = RequestFingerprint.posSaleFinalizeRequestHash(request);
        return idempotencyService.runPosSaleFinalize(
                userId,
                idempotencyKeyHeader,
                fingerprint,
                () -> persistPosSale(userId, request));
    }

    private PosSaleFinalizeResponse persistPosSale(long userId, PosSaleFinalizeRequest request) {
        User user = users.getReferenceById(userId);
        CashSession cashSession =
                cashSessions
                        .findByIdAndUser_Id(request.cashSessionId(), userId)
                        .orElseThrow(CashSessionNotFoundException::new);
        if (cashSession.getStatus() != CashSessionStatus.OPEN) {
            throw new CashSessionNotOpenException();
        }

        List<PosSaleLineRequest> lines = request.resolvedLines();
        List<ResolvedPosLine> resolved = new ArrayList<>(lines.size());
        for (PosSaleLineRequest lineRequest : lines) {
            Product product =
                    products
                            .findByIdAndUser_Id(lineRequest.productId(), userId)
                            .orElseThrow(ProductNotFoundException::new);
            SaleRegistrationLine line =
                    SaleRegistrationLine.fromRequest(
                            new SaleRequest(
                                    lineRequest.productId(),
                                    lineRequest.quantity(),
                                    lineRequest.unitPrice(),
                                    lineRequest.unitCost()),
                            product.getCostPrice());
            resolved.add(new ResolvedPosLine(product, line));
        }

        BigDecimal saleTotal =
                resolved.stream()
                        .map(item -> item.line().unitPrice().multiply(item.line().quantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paymentTotal =
                request.payments().stream()
                        .map(PosSalePaymentRequest::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paymentTotal.compareTo(saleTotal) != 0) {
            throw new PosSalePaymentTotalMismatchException();
        }

        ResolvedPosLine headerLine = resolved.get(0);
        Sale sale = new Sale();
        sale.setUser(user);
        sale.setProduct(headerLine.product());
        sale.setCashSession(cashSession);
        sale.setQuantity(headerLine.line().quantity());
        sale.setUnitPrice(headerLine.line().unitPrice());
        sale.setUnitCost(headerLine.line().unitCost());
        sales.save(sale);

        List<SaleItem> persistedItems = new ArrayList<>();
        for (ResolvedPosLine item : resolved) {
            SaleItem saleItem = new SaleItem();
            saleItem.setUser(user);
            saleItem.setSale(sale);
            saleItem.setProduct(item.product());
            saleItem.setQuantity(item.line().quantity());
            saleItem.setUnitPrice(item.line().unitPrice());
            saleItem.setUnitCost(item.line().unitCost());
            saleItems.save(saleItem);
            persistedItems.add(saleItem);
            inventoryService.decreaseForSale(user, item.product(), item.line().quantity(), sale.getId());
        }

        for (PosSalePaymentRequest paymentRequest : request.payments()) {
            SalePayment payment = new SalePayment();
            payment.setUser(user);
            payment.setSale(sale);
            payment.setPaymentMethod(paymentRequest.paymentMethod());
            payment.setAmount(paymentRequest.amount());
            salePayments.save(payment);
        }

        posSaleCommissionService.assignSellerAndAccrue(
                userId, user, cashSession, sale, persistedItems, request.sellerId());
        sales.save(sale);

        auditService.log(
                userId,
                "POS_SALE_FINALIZED",
                "saleId=%d cashSessionId=%d total=%s lines=%d payments=%d"
                        .formatted(
                                sale.getId(),
                                cashSession.getId(),
                                saleTotal,
                                resolved.size(),
                                request.payments().size()));

        Long sellerId = sale.getSeller() == null ? null : sale.getSeller().getId();
        return new PosSaleFinalizeResponse(
                sale.getId(), cashSession.getId(), saleTotal, sale.getSoldAt(), sellerId);
    }

    private record ResolvedPosLine(Product product, SaleRegistrationLine line) {}
}
