package com.yao.crm.repository;

import com.yao.crm.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> {
    Optional<SubscriptionPlan> findByCode(String code);
    List<SubscriptionPlan> findAllByEnabledTrueOrderByPriceCentsMonthlyAscCodeAsc();
    List<SubscriptionPlan> findAllByOrderByPriceCentsMonthlyAscCodeAsc();
}
