package com.lojapp.repository;

import com.lojapp.entity.InventoryBalance;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

    Optional<InventoryBalance> findByUser_IdAndProduct_Id(Long userId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select b from InventoryBalance b where b.user.id = :userId and b.product.id = :productId")
    Optional<InventoryBalance> lockByUserAndProduct(
            @Param("userId") Long userId, @Param("productId") Long productId);

    List<InventoryBalance> findByUser_IdAndQuantityLessThanOrderByQuantityAsc(
            Long userId, BigDecimal quantity);
}
