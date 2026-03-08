package com.yao.crm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class NotificationDispatchService {

    private final AuditLogService auditLogService;
    private final String providers;

    public NotificationDispatchService(AuditLogService auditLogService,
                                       @Value("${integration.webhooks.providers:WECOM,DINGTALK}") String providers) {
        this.auditLogService = auditLogService;
        this.providers = providers == null ? "" : providers;
    }

    public void dispatchApprovalSlaEscalated(String tenantId, String instanceId, String taskId, String approverRole) {
        String[] all = providers.split(",");
        for (String p : all) {
            String provider = p == null ? "" : p.trim().toUpperCase(Locale.ROOT);
            if (provider.isEmpty()) continue;
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("event", "approval_sla_escalated");
            payload.put("tenantId", tenantId);
            payload.put("instanceId", instanceId);
            payload.put("taskId", taskId);
            payload.put("approverRole", approverRole);
            auditLogService.record("system", "SYSTEM", "WEBHOOK_DISPATCH", provider, taskId, String.valueOf(payload), tenantId);
        }
    }
}
