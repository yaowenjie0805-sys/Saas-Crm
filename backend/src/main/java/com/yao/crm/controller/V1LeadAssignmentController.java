package com.yao.crm.controller;

import com.yao.crm.dto.request.V1LeadAssignmentRuleRequest;
import com.yao.crm.entity.LeadAssignmentRule;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.LeadAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leads/assignment-rules")
public class V1LeadAssignmentController extends BaseApiController {

    private final LeadAssignmentService leadAssignmentService;

    public V1LeadAssignmentController(LeadAssignmentService leadAssignmentService, I18nService i18nService) {
        super(i18nService);
        this.leadAssignmentService = leadAssignmentService;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("items", leadAssignmentService.listRules(tenantId));
        return ResponseEntity.ok(successWithFields(request, "lead_assignment_rules_listed", body));
    }

    @PostMapping
    public ResponseEntity<?> create(HttpServletRequest request, @Valid @RequestBody V1LeadAssignmentRuleRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        LeadAssignmentRule row = leadAssignmentService.createRule(tenantId, currentUser(request), payload);
        return ResponseEntity.status(201).body(successWithFields(request, "lead_assignment_rule_created", leadAssignmentService.toView(row)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody V1LeadAssignmentRuleRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        LeadAssignmentRule row = leadAssignmentService.patchRule(tenantId, id, currentUser(request), payload);
        if (row == null) {
            return ResponseEntity.status(404).body(errorBody(request, "lead_assignment_rule_not_found", msg(request, "lead_assignment_rule_not_found"), null));
        }
        return ResponseEntity.ok(successWithFields(request, "lead_assignment_rule_updated", leadAssignmentService.toView(row)));
    }
}
