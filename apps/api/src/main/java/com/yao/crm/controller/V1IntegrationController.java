package com.yao.crm.controller;

import com.yao.crm.service.AuditLogService;
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

    public V1IntegrationController(AuditLogService auditLogService, I18nService i18nService) {
        super(i18nService);
        this.auditLogService = auditLogService;
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
        auditLogService.record(currentUser(request), currentRole(request), "WEBHOOK", provider, null, String.valueOf(payload), tenantId);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("accepted", true);
        body.put("provider", provider);
        body.put("tenantId", tenantId);
        return ResponseEntity.accepted().body(successWithFields(request, "webhook_accepted", body));
    }
}

