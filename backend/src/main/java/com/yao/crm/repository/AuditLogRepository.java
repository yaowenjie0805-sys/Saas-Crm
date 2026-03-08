package com.yao.crm.repository;

import com.yao.crm.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
