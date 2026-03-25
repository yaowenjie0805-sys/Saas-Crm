package com.yao.crm.controller;

import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.IntegrationWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V1IntegrationControllerTest {

    private AuditLogService auditLogService;
    private IntegrationWebhookService integrationWebhookService;
    private V1IntegrationController controller;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        integrationWebhookService = mock(IntegrationWebhookService.class);
        controller = new V1IntegrationController(auditLogService, integrationWebhookService, new I18nService());
    }

    @Test
    @SuppressWarnings("unchecked")
    void wecomShouldDispatchMessageWhenAuthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authUsername", "boss");
        request.setAttribute("authTenantId", "tenant_default");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("title", "审批升级");
        payload.put("text", "请尽快处理");

        when(integrationWebhookService.sendMessage("WECOM", "tenant_default", "审批升级", "请尽快处理", "boss"))
                .thenReturn(true);

        ResponseEntity<?> response = controller.wecom(request, payload);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("webhook_accepted", body.get("code"));
        assertTrue(Boolean.TRUE.equals(body.get("accepted")));
        assertTrue(Boolean.TRUE.equals(body.get("dispatched")));
        verify(integrationWebhookService).sendMessage("WECOM", "tenant_default", "审批升级", "请尽快处理", "boss");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dingtalkShouldFallbackToPayloadStringWhenMessageFieldsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "ops");
        request.setAttribute("authTenantId", "tenant_cn");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("event", "approval_sla_escalated");

        when(integrationWebhookService.sendMessage(
                eq("DINGTALK"),
                eq("tenant_cn"),
                eq("Webhook DINGTALK"),
                eq(payload.toString()),
                eq("ops")
        )).thenReturn(false);

        ResponseEntity<?> response = controller.dingtalk(request, payload);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(Boolean.TRUE.equals(body.get("accepted")));
        assertFalse(Boolean.TRUE.equals(body.get("dispatched")));
        verify(integrationWebhookService).sendMessage("DINGTALK", "tenant_cn", "Webhook DINGTALK", payload.toString(), "ops");
    }

    @Test
    @SuppressWarnings("unchecked")
    void feishuShouldRejectUnauthorizedCaller() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ANALYST");
        request.setAttribute("authUsername", "viewer");
        request.setAttribute("authTenantId", "tenant_default");

        ResponseEntity<?> response = controller.feishu(request, new LinkedHashMap<String, Object>());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("forbidden", body.get("code"));
        verify(integrationWebhookService, never()).sendMessage(eq("FEISHU"), eq("tenant_default"), eq("Webhook FEISHU"), eq("{}"), eq("viewer"));
    }
}
