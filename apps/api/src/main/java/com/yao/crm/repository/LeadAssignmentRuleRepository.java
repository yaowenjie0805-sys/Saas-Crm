package com.yao.crm.repository;

import com.yao.crm.entity.LeadAssignmentRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadAssignmentRuleRepository extends JpaRepository<LeadAssignmentRule, String> {
    List<LeadAssignmentRule> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
    Optional<LeadAssignmentRule> findByIdAndTenantId(String id, String tenantId);
    Optional<LeadAssignmentRule> findFirstByTenantIdAndEnabledOrderByUpdatedAtDesc(String tenantId, Boolean enabled);
}
