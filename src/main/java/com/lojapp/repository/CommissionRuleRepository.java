package com.lojapp.repository;

import com.lojapp.entity.CommissionRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Long> {

    List<CommissionRule> findByUser_Id(long userId);
}
