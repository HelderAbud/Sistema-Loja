package com.lojapp.repository;

import com.lojapp.entity.SaleItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySale_IdAndUser_Id(long saleId, long userId);
}
