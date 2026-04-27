package com.lojapp.repository;

import com.lojapp.entity.Supplier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByIdAndUser_Id(long id, long userId);

    List<Supplier> findByUser_IdOrderByLegalNameAsc(long userId);

    boolean existsByUser_IdAndCnpj(long userId, String cnpj);

    Optional<Supplier> findByUser_IdAndCnpj(long userId, String cnpj);
}
