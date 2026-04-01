package com.yao.crm.controller;

import com.yao.crm.dto.request.V1AutomationRuleRequest;
import com.yao.crm.dto.request.V1AutomationRulePatchRequest;
import com.yao.crm.entity.AutomationRule;
import com.yao.crm.repository.AutomationRuleRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/automation")
public class V1AutomationController extends BaseApiController {

    private static final Set<String> ALLOWED_TRIGGER_TYPES = new HashSet<String>();
    private static final Set<String> ALLOWED_ACTION_TYPES = new HashSet<String>();

    static {
        ALLOWED_TRIGGER_TYPES.add("LEAD_CREATED");
        ALLOWED_TRIGGER_TYPES.add("LEAD_STATUS_CHANGED");
        ALLOWED_TRIGGER_TYPES.add("LEAD_ASSIGNED");
        ALLOWED_TRIGGER_TYPES.add("FIELD_CHANGE");
        ALLOWED_TRIGGER_TYPES.add("TIME_EVENT");

        ALLOWED_ACTION_TYPES.add("CREATE_TASK");
        ALLOWED_ACTION_TYPES.add("UPDATE_LEAD_STATUS");
        ALLOWED_ACTION_TYPES.add("CREATE_NOTIFICATION");
    }

    private final AutomationRuleRepository ruleRepository;
    private final AuditLogService auditLogService;

    public V1AutomationController(AutomationRuleRepository ruleRepository,
                                  AuditLogService auditLogService,
                                  I18nService i18nService) {
        super(i18nService);
        this.ruleRepository = ruleRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/rules")
    public ResponseEntity<?> listRules(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (AutomationRule row : ruleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)) {
            items.add(toView(row));
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("items", items);
        return ResponseEntity.ok(successWithFields(request, "automation_rules_listed", body));
    }

    @PostMapping("/rules")
    public ResponseEntity<?> createRule(HttpServletRequest request, @Valid @RequestBody V1AutomationRuleRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }

        String tenantId = currentTenant(request);
        String triggerType = normalize(payload.getTriggerType());
        String actionType = normalize(payload.getActionType());
        if (!ALLOWED_TRIGGER_TYPES.contains(triggerType)) {
            return ResponseEntity.badRequest().body(errorBody(request, "automation_trigger_invalid", msg(request, "automation_trigger_invalid"), null));
        }
        if (!ALLOWED_ACTION_TYPES.contains(actionType)) {
            return ResponseEntity.badRequest().body(errorBody(request, "automation_action_invalid", msg(request, "automation_action_invalid"), null));
        }
        AutomationRule rule = new AutomationRule();
        rule.setId("ar_" + Long.toString(System.currentTimeMillis(), 36));
        rule.setTenantId(tenantId);
        rule.setName(payload.getName().trim());
        rule.setTriggerType(triggerType);
        rule.setTriggerExpr(payload.getTriggerExpr().trim());
        rule.setActionType(actionType);
        rule.setActionPayload(payload.getActionPayload().trim());
        rule.setEnabled(true);
        rule = ruleRepository.save(rule);

        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "AUTOMATION_RULE", rule.getId(), "Created automation rule", tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "automation_rule_created", toView(rule)));
    }

    @PatchMapping("/rules/{id}")
    public ResponseEntity<?> patchRule(HttpServletRequest request, @PathVariable String id, @RequestBody V1AutomationRulePatchRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        if (payload == null) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String normalizedId = normalizeId(id);
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = currentTenant(request);
        Optional<AutomationRule> optional = ruleRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "automation_rule_not_found", msg(request, "automation_rule_not_found"), null));
        }
        AutomationRule row = optional.get();
        if (!isBlank(payload.getName())) row.setName(payload.getName().trim());
        if (!isBlank(payload.getTriggerType())) {
            String triggerType = normalize(payload.getTriggerType());
            if (!ALLOWED_TRIGGER_TYPES.contains(triggerType)) {
                return ResponseEntity.badRequest().body(errorBody(request, "automation_trigger_invalid", msg(request, "automation_trigger_invalid"), null));
            }
            row.setTriggerType(triggerType);
        }
        if (!isBlank(payload.getActionType())) {
            String actionType = normalize(payload.getActionType());
            if (!ALLOWED_ACTION_TYPES.contains(actionType)) {
                return ResponseEntity.badRequest().body(errorBody(request, "automation_action_invalid", msg(request, "automation_action_invalid"), null));
            }
            row.setActionType(actionType);
        }
        if (payload.getTriggerExpr() != null) {
            if (isBlank(payload.getTriggerExpr())) {
                return ResponseEntity.badRequest().body(errorBody(request, "automation_trigger_required", msg(request, "automation_trigger_required"), null));
            }
            row.setTriggerExpr(payload.getTriggerExpr().trim());
        }
        if (payload.getActionPayload() != null) {
            if (isBlank(payload.getActionPayload())) {
                return ResponseEntity.badRequest().body(errorBody(request, "automation_action_required", msg(request, "automation_action_required"), null));
            }
            row.setActionPayload(payload.getActionPayload().trim());
        }
        if (payload.getEnabled() != null) row.setEnabled(payload.getEnabled());
        row = ruleRepository.save(row);

        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "AUTOMATION_RULE", row.getId(), "Updated automation rule", tenantId);
        return ResponseEntity.ok(successWithFields(request, "automation_rule_updated", toView(row)));
    }

    private Map<String, Object> toView(AutomationRule rule) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", rule.getId());
        out.put("tenantId", rule.getTenantId());
        out.put("name", rule.getName());
        out.put("triggerType", rule.getTriggerType());
        out.put("triggerExpr", rule.getTriggerExpr());
        out.put("actionType", rule.getActionType());
        out.put("actionPayload", rule.getActionPayload());
        out.put("enabled", rule.getEnabled());
        out.put("createdAt", rule.getCreatedAt());
        out.put("updatedAt", rule.getUpdatedAt());
        return out;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeId(String value) {
        return isBlank(value) ? "" : value.trim();
    }
}

