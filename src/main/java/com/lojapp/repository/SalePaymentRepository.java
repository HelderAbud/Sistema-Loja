package com.lojapp.repository;

import com.lojapp.entity.SalePayment;
import com.lojapp.entity.PaymentMethod;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    @Query(
            """
            select coalesce(sum(sp.amount), 0)
            from SalePayment sp
            where sp.user.id = :userId
              and sp.sale.cashSession.id = :cashSessionId
              and sp.sale.cancelledAt is null
            """)
    BigDecimal sumAmountByCashSession(@Param("userId") Long userId, @Param("cashSessionId") Long cashSessionId);

    @Query(
            """
            select coalesce(sum(sp.amount), 0)
            from SalePayment sp
            where sp.user.id = :userId
              and sp.sale.cashSession.id = :cashSessionId
              and sp.paymentMethod = :paymentMethod
              and sp.sale.cancelledAt is null
            """)
    BigDecimal sumAmountByCashSessionAndMethod(
            @Param("userId") Long userId,
            @Param("cashSessionId") Long cashSessionId,
            @Param("paymentMethod") PaymentMethod paymentMethod);
}
