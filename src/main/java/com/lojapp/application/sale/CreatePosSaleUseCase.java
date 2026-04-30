package com.lojapp.application.sale;

import com.lojapp.application.contract.CreatePosSaleUseCaseContract;
import com.lojapp.application.idempotency.ApiIdempotencyService;
import com.lojapp.application.idempotency.RequestFingerprint;
import com.lojapp.domain.sale.SaleRegistrationLine;
import com.lojapp.dto.sale.PosSaleFinalizeRequest;
import com.lojapp.dto.sale.PosSaleFinalizeResponse;
import com.lojapp.dto.sale.PosSalePaymentRequest;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import com.lojapp.entity.Product;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SalePayment;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.CashSessionNotFoundException;
import com.lojapp.exception.domain.CashSessionNotOpenException;
import com.lojapp.exception.domain.PosSalePaymentTotalMismatchException;
import com.lojapp.exception.domain.ProductNotFoundException;
import com.lojapp.repository.CashSessionRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.SalePaymentRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.contract.InventoryServiceContract;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePosSaleUseCase implements CreatePosSaleUseCaseContract {

    private final UserRepository users;
    private final ProductRepository products;
    private final SaleRepository sales;
    private final SalePaymentRepository salePayments;
    private final CashSessionRepository cashSessions;
    private final InventoryServiceContract inventoryService;
    private final AuditService auditService;
    private final ApiIdempotencyService idempotencyService;

    public CreatePosSaleUseCase(
            UserRepository users,
            ProductRepository products,
            SaleRepository sales,
            SalePaymentRepository salePayments,
            CashSessionRepository cashSessions,
            InventoryServiceContract inventoryService,
            AuditService auditService,
            ApiIdempotencyService idempotencyService) {
        this.users = users;
        this.products = products;
        this.sales = sales;
        this.salePayments = salePayments;
        this.cashSessions = cashSessions;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
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
        Product product =
                products
                        .findByIdAndUser_Id(request.productId(), userId)
                        .orElseThrow(ProductNotFoundException::new);
        CashSession cashSession =
                cashSessions
                        .findByIdAndUser_Id(request.cashSessionId(), userId)
                        .orElseThrow(CashSessionNotFoundException::new);
        if (cashSession.getStatus() != CashSessionStatus.OPEN) {
            throw new CashSessionNotOpenException();
        }

        SaleRegistrationLine line = SaleRegistrationLine.fromRequest(
                new SaleRequest(
                        request.productId(), request.quantity(), request.unitPrice(), request.unitCost()),
                product.getCostPrice());
        BigDecimal saleTotal = line.unitPrice().multiply(line.quantity());
        BigDecimal paymentTotal =
                request.payments().stream()
                        .map(PosSalePaymentRequest::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paymentTotal.compareTo(saleTotal) != 0) {
            throw new PosSalePaymentTotalMismatchException();
        }

        Sale sale = new Sale();
        sale.setUser(user);
        sale.setProduct(product);
        sale.setCashSession(cashSession);
        sale.setQuantity(line.quantity());
        sale.setUnitPrice(line.unitPrice());
        sale.setUnitCost(line.unitCost());
        sales.save(sale);
        inventoryService.decreaseForSale(user, product, line.quantity(), sale.getId());

        for (PosSalePaymentRequest paymentRequest : request.payments()) {
            SalePayment payment = new SalePayment();
            payment.setUser(user);
            payment.setSale(sale);
            payment.setPaymentMethod(paymentRequest.paymentMethod());
            payment.setAmount(paymentRequest.amount());
            salePayments.save(payment);
        }

        auditService.log(
                userId,
                "POS_SALE_FINALIZED",
                "saleId=%d cashSessionId=%d total=%s payments=%d"
                        .formatted(sale.getId(), cashSession.getId(), saleTotal, request.payments().size()));

        return new PosSaleFinalizeResponse(sale.getId(), cashSession.getId(), saleTotal, sale.getSoldAt());
    }
}
