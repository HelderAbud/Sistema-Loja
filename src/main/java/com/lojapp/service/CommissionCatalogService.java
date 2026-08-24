package com.lojapp.service;

import com.lojapp.dto.commission.CommissionRuleRequest;
import com.lojapp.dto.commission.CommissionRuleResponse;
import com.lojapp.dto.seller.SellerRequest;
import com.lojapp.dto.seller.SellerResponse;
import com.lojapp.entity.Brand;
import com.lojapp.entity.CommissionRule;
import com.lojapp.entity.Seller;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.BrandNotFoundException;
import com.lojapp.repository.BrandRepository;
import com.lojapp.repository.CommissionRuleRepository;
import com.lojapp.repository.SellerRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.contract.CommissionCatalogServiceContract;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionCatalogService implements CommissionCatalogServiceContract {

    private final UserRepository users;
    private final SellerRepository sellers;
    private final CommissionRuleRepository rules;
    private final BrandRepository brands;

    public CommissionCatalogService(
            UserRepository users,
            SellerRepository sellers,
            CommissionRuleRepository rules,
            BrandRepository brands) {
        this.users = users;
        this.sellers = sellers;
        this.rules = rules;
        this.brands = brands;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerResponse> listSellers(long userId) {
        return sellers.findByUser_IdOrderBySortOrderAscIdAsc(userId).stream()
                .map(CommissionCatalogService::toSeller)
                .toList();
    }

    @Override
    @Transactional
    public SellerResponse createSeller(long userId, SellerRequest request) {
        User user = users.getReferenceById(userId);
        Seller seller = new Seller();
        seller.setUser(user);
        seller.setDisplayName(request.displayName().trim());
        seller.setActive(request.active() == null || request.active());
        seller.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        sellers.save(seller);
        return toSeller(seller);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommissionRuleResponse> listRules(long userId) {
        return rules.findByUser_Id(userId).stream().map(CommissionCatalogService::toRule).toList();
    }

    @Override
    @Transactional
    public CommissionRuleResponse createRule(long userId, CommissionRuleRequest request) {
        User user = users.getReferenceById(userId);
        CommissionRule rule = new CommissionRule();
        rule.setUser(user);
        if (request.brandId() != null) {
            Brand brand =
                    brands.findByIdAndUser_Id(request.brandId(), userId)
                            .orElseThrow(BrandNotFoundException::new);
            rule.setBrand(brand);
        }
        rule.setPercent(request.percent());
        rule.setValidFrom(request.validFrom() == null ? Instant.now() : request.validFrom());
        rules.save(rule);
        return toRule(rule);
    }

    private static SellerResponse toSeller(Seller seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getDisplayName(),
                seller.isActive(),
                seller.getSortOrder(),
                seller.getCreatedAt());
    }

    private static CommissionRuleResponse toRule(CommissionRule rule) {
        Long brandId = rule.getBrand() == null ? null : rule.getBrand().getId();
        return new CommissionRuleResponse(
                rule.getId(), brandId, rule.getPercent(), rule.getValidFrom(), rule.getCreatedAt());
    }
}
