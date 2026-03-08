package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalTemplateRepository extends JpaRepository<ApprovalTemplate, String> {
    List<ApprovalTemplate> findByTenantIdAndBizTypeAndEnabledTrueOrderByCreatedAtAsc(String tenantId, String bizType);
    List<ApprovalTemplate> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<ApprovalTemplate> findByIdAndTenantId(String id, String tenantId);
}
