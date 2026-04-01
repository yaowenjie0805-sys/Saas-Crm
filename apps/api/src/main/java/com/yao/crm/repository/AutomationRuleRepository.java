package com.yao.crm.repository;

import com.yao.crm.entity.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AutomationRuleRepository extends JpaRepository<AutomationRule, String> {
    List<AutomationRule> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<AutomationRule> findByIdAndTenantId(String id, String tenantId);
}
