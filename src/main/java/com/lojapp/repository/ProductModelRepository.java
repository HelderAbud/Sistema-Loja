package com.lojapp.repository;

import com.lojapp.entity.ProductModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductModelRepository extends JpaRepository<ProductModel, Long> {

    Optional<ProductModel> findByIdAndUser_Id(long id, long userId);

    List<ProductModel> findByUser_IdAndBrand_IdOrderByNameAsc(long userId, long brandId);

    List<ProductModel> findByUser_IdAndBrand_IdAndCollection_IdOrderByNameAsc(
            long userId, long brandId, long collectionId);

    Optional<ProductModel> findByUser_IdAndBrand_IdAndNameIgnoreCase(
            long userId, long brandId, String name);
}
