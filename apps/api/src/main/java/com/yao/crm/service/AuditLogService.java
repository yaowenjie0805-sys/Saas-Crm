package com.yao.crm.service;

import com.yao.crm.entity.AuditLog;
import com.yao.crm.repository.AuditLogRepository;
import com.yao.crm.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final IdGenerator idGenerator;

    public AuditLogService(AuditLogRepository auditLogRepository, IdGenerator idGenerator) {
        this.auditLogRepository = auditLogRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(timeout = 30)
    public void record(String username, String role, String action, String resource, String resourceId, String details) {
        record(username, role, action, resource, resourceId, details, null);
    }

    @Transactional(timeout = 30)
    public void record(String username, String role, String action, String resource, String resourceId, String details, String tenantId) {
        String normalizedTenantId = requireTenantId(tenantId);
        AuditLog log = new AuditLog();
        log.setId(idGenerator.generate("log"));
        log.setUsername(username == null ? "unknown" : username);
        log.setRole(role == null ? "UNKNOWN" : role);
        log.setAction(action);
        log.setResource(resource);
        log.setResourceId(resourceId);
        log.setDetails(details);
        log.setTenantId(normalizedTenantId);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> latest() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> latestByTenant(String tenantId) {
        return auditLogRepository.findTop100ByTenantIdOrderByCreatedAtDesc(requireTenantId(tenantId));
    }

    private String requireTenantId(String tenantId) {
        String normalized = normalizeTenant(tenantId);
        if (normalized != null) {
            return normalized;
        }

        String fromRequest = resolveTenantFromRequestContext();
        if (fromRequest != null) {
            return fromRequest;
        }

        throw new IllegalStateException("tenant_id_required");
    }

    private String resolveTenantFromRequestContext() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }

        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        if (request == null) {
            return null;
        }

        String tenantFromAttr = normalizeTenant(request.getAttribute("authTenantId"));
        if (tenantFromAttr != null) {
            return tenantFromAttr;
        }

        return normalizeTenant(request.getHeader("X-Tenant-Id"));
    }

    private String normalizeTenant(Object tenantId) {
        if (tenantId == null) {
            return null;
        }
        String normalized = String.valueOf(tenantId).trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
