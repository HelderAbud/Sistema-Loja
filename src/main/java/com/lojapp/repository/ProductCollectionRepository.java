package com.lojapp.repository;

import com.lojapp.entity.ProductCollection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCollectionRepository extends JpaRepository<ProductCollection, Long> {

    Optional<ProductCollection> findByIdAndUser_Id(long id, long userId);

    List<ProductCollection> findByUser_IdAndBrand_IdOrderByNameAsc(long userId, long brandId);

    Optional<ProductCollection> findByUser_IdAndBrand_IdAndNameIgnoreCase(
            long userId, long brandId, String name);
}
