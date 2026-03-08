package com.yao.crm.controller;

import com.yao.crm.dto.request.V1AutomationRuleRequest;
import com.yao.crm.entity.AutomationRule;
import com.yao.crm.repository.AutomationRuleRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/automation")
public class V1AutomationController extends BaseApiController {

    private final AutomationRuleRepository ruleRepository;
    private final AuditLogService auditLogService;

    public V1AutomationController(AutomationRuleRepository ruleRepository,
                                  AuditLogService auditLogService,
                                  I18nService i18nService) {
        super(i18nService);
        this.ruleRepository = ruleRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/rules")
    public ResponseEntity<?> createRule(HttpServletRequest request, @Valid @RequestBody V1AutomationRuleRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }

        String tenantId = currentTenant(request);
        AutomationRule rule = new AutomationRule();
        rule.setId("ar_" + Long.toString(System.currentTimeMillis(), 36));
        rule.setTenantId(tenantId);
        rule.setName(payload.getName().trim());
        rule.setTriggerType(payload.getTriggerType().trim().toUpperCase());
        rule.setTriggerExpr(payload.getTriggerExpr().trim());
        rule.setActionType(payload.getActionType().trim().toUpperCase());
        rule.setActionPayload(payload.getActionPayload().trim());
        rule.setEnabled(true);
        rule = ruleRepository.save(rule);

        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "AUTOMATION_RULE", rule.getId(), "Created automation rule", tenantId);
        return ResponseEntity.status(201).body(rule);
    }
}

