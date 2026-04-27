package com.lojapp.repository;

import com.lojapp.entity.ApiIdempotency;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiIdempotencyRepository extends JpaRepository<ApiIdempotency, Long> {

    Optional<ApiIdempotency> findByUser_IdAndScopeAndKeyHash(Long userId, String scope, String keyHash);

    long countByUser_Id(Long userId);

    /**
     * Bloqueio transaccional por par (k1,k2) para serializar o mesmo Idempotency-Key por utilizador
     * e âmbito.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(:k1, :k2)", nativeQuery = true)
    void advisoryXactLock(@Param("k1") int k1, @Param("k2") int k2);

    @Modifying
    @Query("delete from ApiIdempotency a where a.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
