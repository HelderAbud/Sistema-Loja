package com.lojapp.service.contract;

import com.lojapp.dto.commission.CommissionRuleRequest;
import com.lojapp.dto.commission.CommissionRuleResponse;
import com.lojapp.dto.seller.SellerRequest;
import com.lojapp.dto.seller.SellerResponse;
import java.util.List;

public interface CommissionCatalogServiceContract {

    List<SellerResponse> listSellers(long userId);

    SellerResponse createSeller(long userId, SellerRequest request);

    List<CommissionRuleResponse> listRules(long userId);

    CommissionRuleResponse createRule(long userId, CommissionRuleRequest request);
}
