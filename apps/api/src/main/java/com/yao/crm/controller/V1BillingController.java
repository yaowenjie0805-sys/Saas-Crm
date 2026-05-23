package com.yao.crm.controller;

import com.yao.crm.dto.request.UpdateTenantFeatureFlagRequest;
import com.yao.crm.dto.request.UpdateTenantSubscriptionRequest;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.BillingService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
public class V1BillingController extends BaseApiController {

    private final BillingService billingService;
    private final AuditLogService auditLogService;

    public V1BillingController(BillingService billingService,
                               AuditLogService auditLogService,
                               I18nService i18nService) {
        super(i18nService);
        this.billingService = billingService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/plans")
    public ResponseEntity<?> plans(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return forbidden(request);
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("items", billingService.plans());
        return ResponseEntity.ok(successWithFields(request, "billing_plans_loaded", out));
    }

    @GetMapping("/tenants/{tenantId}/subscription")
    public ResponseEntity<?> subscription(HttpServletRequest request, @PathVariable String tenantId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return forbidden(request);
        }
        if (!isCurrentTenant(request, tenantId)) {
            return tenantMismatch(request);
        }
        try {
            return ResponseEntity.ok(successWithFields(request, "billing_subscription_loaded", billingService.subscription(tenantId.trim())));
        } catch (IllegalArgumentException ex) {
            return badRequest(request, ex.getMessage());
        }
    }

    @PutMapping("/tenants/{tenantId}/subscription")
    public ResponseEntity<?> updateSubscription(HttpServletRequest request,
                                                @PathVariable String tenantId,
                                                @RequestBody UpdateTenantSubscriptionRequest payload) {
        if (!hasAnyRole(request, "ADMIN")) {
            return forbidden(request);
        }
        if (!isCurrentTenant(request, tenantId)) {
            return tenantMismatch(request);
        }
        try {
            Map<String, Object> body = billingService.updateSubscription(
                    tenantId.trim(),
                    payload == null ? null : payload.getPlanCode(),
                    payload == null ? null : payload.getStatus(),
                    parseDateTime(payload == null ? null : payload.getExpiresAt()),
                    parseDateTime(payload == null ? null : payload.getTrialEndsAt())
            );
            auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "BILLING_SUBSCRIPTION", tenantId.trim(), "Updated tenant subscription", tenantId.trim());
            return ResponseEntity.ok(successWithFields(request, "billing_subscription_updated", body));
        } catch (IllegalArgumentException ex) {
            return badRequest(request, ex.getMessage());
        }
    }

    @GetMapping("/tenants/{tenantId}/usage")
    public ResponseEntity<?> usage(HttpServletRequest request,
                                   @PathVariable String tenantId,
                                   @RequestParam(defaultValue = "") String startDate,
                                   @RequestParam(defaultValue = "") String endDate) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return forbidden(request);
        }
        if (!isCurrentTenant(request, tenantId)) {
            return tenantMismatch(request);
        }
        try {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("items", billingService.usage(tenantId.trim(), parseLocalDate(startDate), parseLocalDate(endDate)));
            return ResponseEntity.ok(successWithFields(request, "billing_usage_loaded", out));
        } catch (IllegalArgumentException ex) {
            return badRequest(request, ex.getMessage());
        }
    }

    @PostMapping("/tenants/{tenantId}/usage/recalculate")
    public ResponseEntity<?> recalculateUsage(HttpServletRequest request,
                                              @PathVariable String tenantId,
                                              @RequestParam(defaultValue = "") String usageDate) {
        if (!hasAnyRole(request, "ADMIN")) {
            return forbidden(request);
        }
        if (!isCurrentTenant(request, tenantId)) {
            return tenantMismatch(request);
        }
        try {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("items", billingService.recalculateUsage(tenantId.trim(), parseLocalDate(usageDate)));
            auditLogService.record(currentUser(request), currentRole(request), "RECALCULATE", "BILLING_USAGE", tenantId.trim(), "Recalculated tenant usage", tenantId.trim());
            return ResponseEntity.ok(successWithFields(request, "billing_usage_recalculated", out));
        } catch (IllegalArgumentException ex) {
            return badRequest(request, ex.getMessage());
        }
    }

    @GetMapping("/tenants/{tenantId}/features")
    public ResponseEntity<?> features(HttpServletRequest request, @PathVariable String tenantId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return forbidden(request);
        }
        if (!isCurrentTenant(request, tenantId)) {
            return tenantMismatch(request);
        }
        try {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("features", billingService.features(tenantId.trim()));
            return ResponseEntity.ok(successWithFields(request, "billing_features_loaded", out));
        } catch (IllegalArgumentException ex) {
            return badRequest(request, ex.getMessage());
        }
    }

    @PutMapping("/tenants/{tenantId}/features/{flagKey}")
    public ResponseEntity<?> updateFeature(HttpServletRequest request,
                                           @PathVariable String tenantId,
                                           @PathVariable String flagKey,
                                           @RequestBody UpdateTenantFeatureFlagRequest payload) {
        if (!hasAnyRole(request, "ADMIN")) {
            return forbidden(request);
        }
        if (!isCurrentTenant(request, tenantId)) {
            return tenantMismatch(request);
        }
        if (payload == null || payload.getEnabled() == null) {
            return badRequest(request, "feature_flag_enabled_required");
        }
        try {
            Map<String, Object> body = billingService.updateFeature(tenantId.trim(), flagKey, payload.getEnabled(), currentUser(request));
            auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "FEATURE_FLAG", flagKey, "Updated tenant feature flag", tenantId.trim());
            return ResponseEntity.ok(successWithFields(request, "billing_feature_updated", body));
        } catch (IllegalArgumentException ex) {
            return badRequest(request, ex.getMessage());
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) return null;
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid_datetime");
        }
    }

    private LocalDate parseLocalDate(String value) {
        if (isBlank(value)) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid_date");
        }
    }

    private boolean isCurrentTenant(HttpServletRequest request, String tenantId) {
        return !isBlank(tenantId) && tenantId.trim().equals(currentTenant(request));
    }

    private ResponseEntity<?> forbidden(HttpServletRequest request) {
        return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
    }

    private ResponseEntity<?> tenantMismatch(HttpServletRequest request) {
        return ResponseEntity.status(403).body(errorBody(request, "tenant_mismatch", msg(request, "tenant_mismatch"), null));
    }

    private ResponseEntity<?> badRequest(HttpServletRequest request, String code) {
        String normalized = normalizeCode(code, "bad_request");
        return ResponseEntity.badRequest().body(errorBody(request, normalized, msg(request, normalized), null));
    }
}
