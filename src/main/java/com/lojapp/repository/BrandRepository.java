package com.lojapp.repository;

import com.lojapp.entity.Brand;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByUser_IdOrderByNameAsc(Long userId);

    Optional<Brand> findByIdAndUser_Id(Long id, Long userId);

    Optional<Brand> findByUser_IdAndNameIgnoreCase(Long userId, String name);
}
