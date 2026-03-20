package com.yao.crm.repository;

import com.yao.crm.entity.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutomationRuleRepository extends JpaRepository<AutomationRule, String> {
    List<AutomationRule> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
