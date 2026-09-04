package com.lojapp.repository;

import com.lojapp.entity.CommissionAccrual;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommissionAccrualRepository extends JpaRepository<CommissionAccrual, Long> {

    @Query(
            """
            select a from CommissionAccrual a
            join fetch a.seller
            join fetch a.sale
            left join fetch a.brand
            where a.user.id = :userId
              and a.createdAt >= :from
              and a.createdAt <= :to
              and a.sale.cancelledAt is null
            order by a.createdAt desc
            """)
    List<CommissionAccrual> findByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("from") Instant from, @Param("to") Instant to);

    void deleteBySale_IdAndUser_Id(Long saleId, Long userId);
}
