package com.yao.crm.controller;

import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.IntegrationWebhookService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations/webhooks")
public class V1IntegrationController extends BaseApiController {

    private final AuditLogService auditLogService;
    private final IntegrationWebhookService integrationWebhookService;

    public V1IntegrationController(AuditLogService auditLogService,
                                   IntegrationWebhookService integrationWebhookService,
                                   I18nService i18nService) {
        super(i18nService);
        this.auditLogService = auditLogService;
        this.integrationWebhookService = integrationWebhookService;
    }

    @PostMapping("/wecom")
    public ResponseEntity<?> wecom(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        return acceptWebhook(request, "WECOM", payload);
    }

    @PostMapping("/dingtalk")
    public ResponseEntity<?> dingtalk(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        return acceptWebhook(request, "DINGTALK", payload);
    }

    @PostMapping("/feishu")
    public ResponseEntity<?> feishu(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        return acceptWebhook(request, "FEISHU", payload);
    }

    private ResponseEntity<?> acceptWebhook(HttpServletRequest request, String provider, Map<String, Object> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Map<String, Object> safePayload = payload == null ? Collections.<String, Object>emptyMap() : payload;
        auditLogService.record(currentUser(request), currentRole(request), "WEBHOOK", provider, null, String.valueOf(safePayload), tenantId);
        boolean dispatched = integrationWebhookService.sendMessage(
                provider,
                tenantId,
                resolveTitle(provider, safePayload),
                resolveContent(safePayload),
                currentUser(request)
        );
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("accepted", true);
        body.put("dispatched", dispatched);
        body.put("provider", provider);
        body.put("tenantId", tenantId);
        return ResponseEntity.accepted().body(successWithFields(request, "webhook_accepted", body));
    }

    private String resolveTitle(String provider, Map<String, Object> payload) {
        String title = readText(payload, "title", "subject");
        if (!isBlank(title)) {
            return title;
        }
        return "Webhook " + provider;
    }

    private String resolveContent(Map<String, Object> payload) {
        String content = readText(payload, "text", "content", "message", "body");
        if (!isBlank(content)) {
            return content;
        }
        return String.valueOf(payload);
    }

    private String readText(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }
}

