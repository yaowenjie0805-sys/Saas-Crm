package com.yao.crm.controller;

import com.yao.crm.dto.request.V1AutomationRulePatchRequest;
import com.yao.crm.entity.AutomationRule;
import com.yao.crm.repository.AutomationRuleRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V1AutomationControllerTest {

    private AutomationRuleRepository ruleRepository;
    private V1AutomationController controller;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(AutomationRuleRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        controller = new V1AutomationController(ruleRepository, auditLogService, new I18nService());
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchRuleShouldRejectBlankTriggerExprWhenProvided() {
        MockHttpServletRequest request = authedRequest("ADMIN");
        AutomationRule existing = existingRule("ar_1");
        when(ruleRepository.findByTenantIdOrderByCreatedAtDesc("tenant_default")).thenReturn(Collections.singletonList(existing));

        V1AutomationRulePatchRequest payload = new V1AutomationRulePatchRequest();
        payload.setTriggerExpr("   ");

        ResponseEntity<?> response = controller.patchRule(request, "ar_1", payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("automation_trigger_required", body.get("code"));
        verify(ruleRepository, never()).save(any(AutomationRule.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchRuleShouldTrimIdBeforeLookup() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        AutomationRule existing = existingRule("ar_1");
        when(ruleRepository.findByTenantIdOrderByCreatedAtDesc("tenant_default")).thenReturn(Collections.singletonList(existing));
        when(ruleRepository.save(any(AutomationRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        V1AutomationRulePatchRequest payload = new V1AutomationRulePatchRequest();
        payload.setName("  Updated Name  ");

        ResponseEntity<?> response = controller.patchRule(request, "  ar_1  ", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ar_1", body.get("id"));
        assertEquals("Updated Name", body.get("name"));
        verify(ruleRepository).save(existing);
    }

    private AutomationRule existingRule(String id) {
        AutomationRule rule = new AutomationRule();
        rule.setId(id);
        rule.setTenantId("tenant_default");
        rule.setName("Name");
        rule.setTriggerType("LEAD_CREATED");
        rule.setTriggerExpr("{\"expr\":1}");
        rule.setActionType("CREATE_TASK");
        rule.setActionPayload("{\"task\":\"x\"}");
        rule.setEnabled(true);
        return rule;
    }

    private MockHttpServletRequest authedRequest(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", role);
        request.setAttribute("authUsername", "alice");
        request.setAttribute("authTenantId", "tenant_default");
        return request;
    }
}
