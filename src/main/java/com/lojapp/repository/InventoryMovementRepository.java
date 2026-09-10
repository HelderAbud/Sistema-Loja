package com.lojapp.repository;

import com.lojapp.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findByUser_IdAndProduct_IdOrderByCreatedAtDesc(
            Long userId, Long productId, Pageable pageable);
}
