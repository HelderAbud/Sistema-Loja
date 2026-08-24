package com.lojapp.repository;

import com.lojapp.entity.Seller;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    List<Seller> findByUser_IdAndActiveTrueOrderBySortOrderAscIdAsc(long userId);

    List<Seller> findByUser_IdOrderBySortOrderAscIdAsc(long userId);

    Optional<Seller> findByIdAndUser_Id(long id, long userId);
}
