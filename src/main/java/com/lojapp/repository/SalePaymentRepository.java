package com.lojapp.repository;

import com.lojapp.entity.SalePayment;
import com.lojapp.entity.PaymentMethod;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    @Query(
            """
            select coalesce(sum(sp.amount), 0)
            from SalePayment sp
            where sp.user.id = :userId
              and sp.settlementCashSession.id = :cashSessionId
              and sp.sale.cancelledAt is null
              and sp.settlementStatus = com.lojapp.entity.PaymentSettlementStatus.CONFIRMED
            """)
    BigDecimal sumAmountByCashSession(@Param("userId") Long userId, @Param("cashSessionId") Long cashSessionId);

    @Query(
            """
            select coalesce(sum(sp.amount), 0)
            from SalePayment sp
            where sp.user.id = :userId
              and sp.settlementCashSession.id = :cashSessionId
              and sp.paymentMethod = :paymentMethod
              and sp.sale.cancelledAt is null
              and sp.settlementStatus = com.lojapp.entity.PaymentSettlementStatus.CONFIRMED
            """)
    BigDecimal sumAmountByCashSessionAndMethod(
            @Param("userId") Long userId,
            @Param("cashSessionId") Long cashSessionId,
            @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query(
            """
            select coalesce(sum(sp.amount), 0)
            from SalePayment sp
            where sp.user.id = :userId
              and sp.settlementCashSession.id = :cashSessionId
              and sp.paymentMethod in :paymentMethods
              and sp.sale.cancelledAt is null
              and sp.settlementStatus = com.lojapp.entity.PaymentSettlementStatus.CONFIRMED
            """)
    BigDecimal sumAmountByCashSessionAndMethods(
            @Param("userId") Long userId,
            @Param("cashSessionId") Long cashSessionId,
            @Param("paymentMethods") Collection<PaymentMethod> paymentMethods);

    @Query(
            """
            select sp from SalePayment sp
            join fetch sp.sale s
            left join fetch s.cashSession
            left join fetch sp.settlementCashSession
            where sp.user.id = :userId
              and sp.settlementStatus = com.lojapp.entity.PaymentSettlementStatus.PENDING
              and s.cancelledAt is null
            order by sp.id
            """)
    List<SalePayment> findPendingUncancelled(@Param("userId") Long userId);

    @Query(
            """
            select sp from SalePayment sp
            join fetch sp.sale s
            left join fetch s.cashSession
            left join fetch sp.settlementCashSession
            where sp.id = :paymentId and sp.user.id = :userId
            """)
    Optional<SalePayment> findByIdAndUserIdWithSale(
            @Param("paymentId") Long paymentId, @Param("userId") Long userId);
}
