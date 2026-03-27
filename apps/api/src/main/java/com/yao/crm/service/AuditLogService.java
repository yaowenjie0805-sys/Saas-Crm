package com.yao.crm.service;

import com.yao.crm.entity.AuditLog;
import com.yao.crm.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(timeout = 30)
    public void record(String username, String role, String action, String resource, String resourceId, String details) {
        record(username, role, action, resource, resourceId, details, "tenant_default");
    }

    @Transactional(timeout = 30)
    public void record(String username, String role, String action, String resource, String resourceId, String details, String tenantId) {
        AuditLog log = new AuditLog();
        log.setId(newId("log"));
        log.setUsername(username == null ? "unknown" : username);
        log.setRole(role == null ? "UNKNOWN" : role);
        log.setAction(action);
        log.setResource(resource);
        log.setResourceId(resourceId);
        log.setDetails(details);
        log.setTenantId((tenantId == null || tenantId.trim().isEmpty()) ? "tenant_default" : tenantId.trim());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> latest() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> latestByTenant(String tenantId) {
        return auditLogRepository.findTop100ByTenantIdOrderByCreatedAtDesc(
                (tenantId == null || tenantId.trim().isEmpty()) ? "tenant_default" : tenantId.trim()
        );
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }
}
