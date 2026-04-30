package com.lojapp.repository;

import com.lojapp.entity.CashSession;
import com.lojapp.entity.CashSessionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashSessionRepository extends JpaRepository<CashSession, Long> {

    Optional<CashSession> findByUser_IdAndStatus(Long userId, CashSessionStatus status);

    Optional<CashSession> findByIdAndUser_Id(Long id, Long userId);
}
