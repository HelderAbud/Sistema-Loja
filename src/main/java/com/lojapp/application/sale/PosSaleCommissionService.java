package com.lojapp.application.sale;

import com.lojapp.application.contract.PosSaleCommissionServiceContract;
import com.lojapp.domain.commission.CommissionAmount;
import com.lojapp.domain.commission.CommissionRulePicker;
import com.lojapp.domain.commission.CommissionRuleSnapshot;
import com.lojapp.domain.commission.SellerRoundRobin;
import com.lojapp.entity.Brand;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CommissionAccrual;
import com.lojapp.entity.CommissionRule;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SaleItem;
import com.lojapp.entity.Seller;
import com.lojapp.entity.SellerQueueState;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.SellerInactiveException;
import com.lojapp.exception.domain.SellerNotFoundException;
import com.lojapp.repository.CommissionAccrualRepository;
import com.lojapp.repository.CommissionRuleRepository;
import com.lojapp.repository.SellerQueueStateRepository;
import com.lojapp.repository.SellerRepository;
import com.lojapp.service.AuditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosSaleCommissionService implements PosSaleCommissionServiceContract {

    private final SellerRepository sellers;
    private final SellerQueueStateRepository queueStates;
    private final CommissionRuleRepository rules;
    private final CommissionAccrualRepository accruals;
    private final AuditService auditService;

    public PosSaleCommissionService(
            SellerRepository sellers,
            SellerQueueStateRepository queueStates,
            CommissionRuleRepository rules,
            CommissionAccrualRepository accruals,
            AuditService auditService) {
        this.sellers = sellers;
        this.queueStates = queueStates;
        this.rules = rules;
        this.accruals = accruals;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void assignSellerAndAccrue(
            long userId,
            User user,
            CashSession cashSession,
            Sale sale,
            List<SaleItem> items,
            Long requestedSellerId) {
        Seller seller = resolveSeller(userId, user, cashSession, requestedSellerId);
        if (seller == null) {
            return;
        }
        sale.setSeller(seller);
        Instant at = sale.getSoldAt() == null ? Instant.now() : sale.getSoldAt();
        List<CommissionRuleSnapshot> snapshots =
                rules.findByUser_Id(userId).stream()
                        .map(
                                r ->
                                        new CommissionRuleSnapshot(
                                                r.getId(),
                                                r.getBrand() == null ? null : r.getBrand().getId(),
                                                r.getPercent(),
                                                r.getValidFrom()))
                        .toList();
        for (SaleItem item : items) {
            Brand brand = item.getProduct().getBrand();
            Long brandId = brand == null ? null : brand.getId();
            BigDecimal base =
                    item.getUnitPrice().multiply(item.getQuantity()).setScale(2, java.math.RoundingMode.HALF_UP);
            var picked = CommissionRulePicker.pick(snapshots, brandId, at);
            if (picked.isEmpty()) {
                continue;
            }
            CommissionRuleSnapshot snap = picked.get();
            CommissionAccrual row = new CommissionAccrual();
            row.setUser(user);
            row.setSale(sale);
            row.setSaleItem(item);
            row.setSeller(seller);
            CommissionRule persisted = rules.getReferenceById(snap.id());
            row.setCommissionRule(persisted);
            row.setBrand(brand);
            row.setBaseAmount(base);
            row.setPercent(snap.percent());
            row.setAmount(CommissionAmount.of(base, snap.percent()));
            accruals.save(row);
        }
    }

    private Seller resolveSeller(
            long userId, User owner, CashSession cashSession, Long requestedSellerId) {
        if (requestedSellerId != null) {
            Seller chosen =
                    sellers.findByIdAndUser_Id(requestedSellerId, userId)
                            .orElseThrow(SellerNotFoundException::new);
            if (!chosen.isActive()) {
                throw new SellerInactiveException();
            }
            if (cashSession != null) {
                rememberAssignment(owner, cashSession, chosen);
            }
            String details =
                    cashSession == null
                            ? "cashSessionId= sellerId=%d manual=true".formatted(chosen.getId())
                            : "cashSessionId=%d sellerId=%d manual=true"
                                    .formatted(cashSession.getId(), chosen.getId());
            auditService.log(userId, "SALE_SELLER_CHANGED", details);
            return chosen;
        }
        if (cashSession == null) {
            return null;
        }
        List<Seller> active = sellers.findByUser_IdAndActiveTrueOrderBySortOrderAscIdAsc(userId);
        List<Long> ids = active.stream().map(Seller::getId).toList();
        SellerQueueState state = queueStates.findById(cashSession.getId()).orElse(null);
        Long lastId = state == null || state.getLastAssignedSeller() == null
                ? null
                : state.getLastAssignedSeller().getId();
        Long nextId = SellerRoundRobin.nextId(ids, lastId);
        if (nextId == null) {
            return null;
        }
        Seller next = active.stream().filter(s -> s.getId().equals(nextId)).findFirst().orElse(null);
        if (next != null) {
            rememberAssignment(owner, cashSession, next);
        }
        return next;
    }

    private void rememberAssignment(User owner, CashSession cashSession, Seller seller) {
        SellerQueueState state =
                queueStates
                        .findById(cashSession.getId())
                        .orElseGet(
                                () -> {
                                    SellerQueueState created = new SellerQueueState();
                                    created.setCashSessionId(cashSession.getId());
                                    created.setUser(owner);
                                    return created;
                                });
        state.setUser(owner);
        state.setLastAssignedSeller(seller);
        state.setUpdatedAt(Instant.now());
        queueStates.save(state);
    }
}
