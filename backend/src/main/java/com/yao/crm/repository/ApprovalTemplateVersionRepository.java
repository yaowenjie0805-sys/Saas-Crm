package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalTemplateVersionRepository extends JpaRepository<ApprovalTemplateVersion, String> {
    List<ApprovalTemplateVersion> findByTenantIdAndTemplateIdOrderByVersionDesc(String tenantId, String templateId);
    Optional<ApprovalTemplateVersion> findByTenantIdAndTemplateIdAndVersion(String tenantId, String templateId, Integer version);
}
