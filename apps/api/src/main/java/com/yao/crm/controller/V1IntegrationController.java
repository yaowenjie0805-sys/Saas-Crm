package com.yao.crm.controller;

import com.yao.crm.dto.request.WebhookRequest;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.IntegrationWebhookService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/integrations/webhooks")
public class V1IntegrationController extends BaseApiController {

    private final AuditLogService auditLogService;
    private final IntegrationWebhookService integrationWebhookService;
    private final TenantRepository tenantRepository;

    public V1IntegrationController(AuditLogService auditLogService,
                                   IntegrationWebhookService integrationWebhookService,
                                   TenantRepository tenantRepository,
                                   I18nService i18nService) {
        super(i18nService);
        this.auditLogService = auditLogService;
        this.integrationWebhookService = integrationWebhookService;
        this.tenantRepository = tenantRepository;
    }

    @PostMapping("/wecom")
    public ResponseEntity<?> wecom(HttpServletRequest request, @Valid @RequestBody WebhookRequest payload) {
        return acceptWebhook(request, "WECOM", payload);
    }

    @PostMapping("/dingtalk")
    public ResponseEntity<?> dingtalk(HttpServletRequest request, @Valid @RequestBody WebhookRequest payload) {
        return acceptWebhook(request, "DINGTALK", payload);
    }

    @PostMapping("/feishu")
    public ResponseEntity<?> feishu(HttpServletRequest request, @Valid @RequestBody WebhookRequest payload) {
        return acceptWebhook(request, "FEISHU", payload);
    }

    private ResponseEntity<?> acceptWebhook(HttpServletRequest request, String provider, WebhookRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        WebhookRequest safePayload = payload;
        if (safePayload == null) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }

        String providerCode = normalizeProvider(provider);
        if (isBlank(providerCode)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }

        String authTenantId = normalize(readAttr(request, "authTenantId"));
        String headerTenantId = normalize(request.getHeader("X-Tenant-Id"));
        if (!isBlank(authTenantId) && !isBlank(headerTenantId) && !authTenantId.equals(headerTenantId)) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("authTenantId", authTenantId);
            details.put("headerTenantId", headerTenantId);
            return ResponseEntity.status(409).body(errorBody(request, "tenant_conflict", msg(request, "tenant_conflict"), details));
        }

        String tenantId = normalize(currentTenant(request));
        if (isBlank(tenantId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        Optional<?> tenant = tenantRepository.findById(tenantId);
        if (!tenant.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "tenant_not_found", msg(request, "tenant_not_found"), null));
        }

        String normalizedUser = normalizeUser(currentUser(request));
        String title = resolveTitle(providerCode, safePayload);
        String content = resolveContent(safePayload);
        IntegrationWebhookService.DispatchResult dispatchResult = integrationWebhookService.sendMessageDetailed(
                providerCode,
                tenantId,
                title,
                content,
                normalizedUser
        );
        Map<String, Object> auditDetails = new LinkedHashMap<String, Object>();
        auditDetails.put("requestId", traceId(request));
        auditDetails.put("retryable", dispatchResult.isRetryable());
        auditDetails.put("dispatched", dispatchResult.isSuccess());
        auditDetails.put("payload", String.valueOf(safePayload));
        auditLogService.record(normalizedUser, currentRole(request), "WEBHOOK", providerCode, null, String.valueOf(auditDetails), tenantId);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("accepted", true);
        body.put("dispatched", dispatchResult.isSuccess());
        body.put("retryable", dispatchResult.isRetryable());
        body.put("provider", providerCode);
        body.put("tenantId", tenantId);
        return ResponseEntity.accepted().body(successWithFields(request, "webhook_accepted", body));
    }

    private String resolveTitle(String provider, WebhookRequest payload) {
        String title = readText(payload.getTitle(), payload.getSubject());
        if (!isBlank(title)) {
            return title;
        }
        return "Webhook " + provider;
    }

    private String resolveContent(WebhookRequest payload) {
        String content = readText(payload.getText(), payload.getContent(), payload.getMessage(), payload.getBody());
        if (!isBlank(content)) {
            return content;
        }
        return "{}";
    }

    private String readText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String text = value.trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }

    private String normalizeProvider(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        return normalized;
    }

    private String normalizeUser(String value) {
        String normalized = normalize(value);
        return isBlank(normalized) ? "unknown" : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String readAttr(HttpServletRequest request, String key) {
        Object value = request.getAttribute(key);
        return value == null ? "" : String.valueOf(value);
    }
}
