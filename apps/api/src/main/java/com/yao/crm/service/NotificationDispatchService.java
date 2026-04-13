package com.yao.crm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
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
            IntegrationWebhookService.DispatchResult result = integrationWebhookService.sendEventDetailed(provider, tenantId, "approval_sla_escalated", String.valueOf(payload), taskId);
            auditLogService.record("system", "SYSTEM", "WEBHOOK_DISPATCH", provider, taskId,
                    auditDetails(result, true, payload), tenantId);
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
            IntegrationWebhookService.DispatchResult result = integrationWebhookService.sendEventDetailed(provider, tenantId, eventType, String.valueOf(fullPayload), eventType);
            auditLogService.record("system", "SYSTEM", "NOTIFICATION_SEND", provider, eventType,
                    auditDetails(result, false, fullPayload), tenantId);
        }
    }

    private String auditDetails(IntegrationWebhookService.DispatchResult result, boolean systemEvent, Map<String, Object> payload) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("requestId", currentRequestId());
        details.put("retryable", result != null && result.isRetryable());
        details.put("sent", result != null && result.isSuccess());
        details.put("eventScope", systemEvent ? "workflow" : "notification");
        details.put("payload", String.valueOf(payload));
        return String.valueOf(details);
    }

    private String currentRequestId() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            if (request != null) {
                Object trace = request.getAttribute(com.yao.crm.security.TraceIdInterceptor.TRACE_ID_ATTR);
                if (trace != null) {
                    String value = String.valueOf(trace).trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return "system";
    }
}
