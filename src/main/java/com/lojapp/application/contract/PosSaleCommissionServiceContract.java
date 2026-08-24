package com.lojapp.application.contract;

import com.lojapp.entity.CashSession;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SaleItem;
import com.lojapp.entity.User;
import java.util.List;

public interface PosSaleCommissionServiceContract {

    void assignSellerAndAccrue(
            long userId,
            User user,
            CashSession cashSession,
            Sale sale,
            List<SaleItem> items,
            Long requestedSellerId);
}
