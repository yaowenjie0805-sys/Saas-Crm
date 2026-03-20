package com.yao.crm.repository;

import com.yao.crm.entity.ReportDesignerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportDesignerTemplateRepository extends JpaRepository<ReportDesignerTemplate, String> {
    Optional<ReportDesignerTemplate> findByIdAndTenantId(String id, String tenantId);
    List<ReportDesignerTemplate> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
    boolean existsByTenantIdAndName(String tenantId, String name);
    boolean existsByTenantIdAndNameAndIdNot(String tenantId, String name, String id);
}
