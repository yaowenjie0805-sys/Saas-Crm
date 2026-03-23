package com.yao.crm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class NotificationDispatchService {

    private final AuditLogService auditLogService;
    private final IntegrationWebhookService integrationWebhookService;
    private final String providers;

    public NotificationDispatchService(AuditLogService auditLogService,
                                       IntegrationWebhookService integrationWebhookService,
                                       @Value("${integration.webhooks.providers:WECOM,DINGTALK}") String providers) {
        this.auditLogService = auditLogService;
        this.integrationWebhookService = integrationWebhookService;
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
            boolean sent = integrationWebhookService.sendEvent(provider, tenantId, "approval_sla_escalated", String.valueOf(payload), taskId);
            auditLogService.record("system", "SYSTEM", "WEBHOOK_DISPATCH", provider, taskId,
                    "sent=" + sent + ", payload=" + String.valueOf(payload), tenantId);
        }
    }

    public void sendNotification(String tenantId, String eventType, Map<String, Object> payload) {
        String[] all = providers.split(",");
        for (String p : all) {
            String provider = p == null ? "" : p.trim().toUpperCase(Locale.ROOT);
            if (provider.isEmpty()) continue;
            Map<String, Object> fullPayload = new LinkedHashMap<String, Object>(payload);
            fullPayload.put("event", eventType);
            fullPayload.put("tenantId", tenantId);
            boolean sent = integrationWebhookService.sendEvent(provider, tenantId, eventType, String.valueOf(fullPayload), eventType);
            auditLogService.record("system", "SYSTEM", "NOTIFICATION_SEND", provider, eventType,
                    "sent=" + sent + ", payload=" + String.valueOf(fullPayload), tenantId);
        }
    }
}
