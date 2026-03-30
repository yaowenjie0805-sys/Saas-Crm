package com.yao.crm.controller;

import com.yao.crm.entity.Tenant;
import com.yao.crm.dto.request.WebhookRequest;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.IntegrationWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1IntegrationControllerTest {

    private AuditLogService auditLogService;
    private IntegrationWebhookService integrationWebhookService;
    private TenantRepository tenantRepository;
    private V1IntegrationController controller;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        integrationWebhookService = mock(IntegrationWebhookService.class);
        tenantRepository = mock(TenantRepository.class);
        controller = new V1IntegrationController(auditLogService, integrationWebhookService, tenantRepository, new I18nService());
    }

    @Test
    @SuppressWarnings("unchecked")
    void wecomShouldTrimParametersBeforeDispatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authUsername", "  boss  ");
        request.setAttribute("authTenantId", "  tenant_default  ");

        WebhookRequest payload = new WebhookRequest();
        payload.setTitle("  Approval Escalation  ");
        payload.setText("  Please handle now  ");

        when(tenantRepository.findById("tenant_default")).thenReturn(Optional.of(mock(Tenant.class)));
        when(integrationWebhookService.sendMessage("WECOM", "tenant_default", "Approval Escalation", "Please handle now", "boss"))
                .thenReturn(true);

        ResponseEntity<?> response = controller.wecom(request, payload);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("webhook_accepted", body.get("code"));
        assertTrue(Boolean.TRUE.equals(body.get("accepted")));
        assertTrue(Boolean.TRUE.equals(body.get("dispatched")));
        assertEquals("WECOM", body.get("provider"));
        assertEquals("tenant_default", body.get("tenantId"));
        verify(integrationWebhookService).sendMessage("WECOM", "tenant_default", "Approval Escalation", "Please handle now", "boss");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dingtalkShouldUseBodyWhenTextFieldsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "ops");
        request.setAttribute("authTenantId", "tenant_cn");

        WebhookRequest payload = new WebhookRequest();
        payload.setBody("{\"event\":\"approval_sla_escalated\"}");

        when(tenantRepository.findById("tenant_cn")).thenReturn(Optional.of(mock(Tenant.class)));
        when(integrationWebhookService.sendMessage(
                eq("DINGTALK"),
                eq("tenant_cn"),
                eq("Webhook DINGTALK"),
                eq("{\"event\":\"approval_sla_escalated\"}"),
                eq("ops")
        )).thenReturn(false);

        ResponseEntity<?> response = controller.dingtalk(request, payload);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(Boolean.TRUE.equals(body.get("accepted")));
        assertFalse(Boolean.TRUE.equals(body.get("dispatched")));
        verify(integrationWebhookService).sendMessage("DINGTALK", "tenant_cn", "Webhook DINGTALK", "{\"event\":\"approval_sla_escalated\"}", "ops");
    }

    @Test
    @SuppressWarnings("unchecked")
    void wecomShouldRejectNullPayload() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authUsername", "boss");
        request.setAttribute("authTenantId", "tenant_default");

        ResponseEntity<?> response = controller.wecom(request, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("bad_request", body.get("code"));
        verifyNoInteractions(tenantRepository, integrationWebhookService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void wecomShouldReturnNotFoundWhenTenantMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authUsername", "boss");
        request.setAttribute("authTenantId", "tenant_missing");

        WebhookRequest payload = new WebhookRequest();
        payload.setText("hello");
        when(tenantRepository.findById("tenant_missing")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.wecom(request, payload);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("tenant_not_found", body.get("code"));
        verify(tenantRepository).findById("tenant_missing");
        verifyNoInteractions(integrationWebhookService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void wecomShouldReturnConflictWhenTenantHeaderConflicts() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authUsername", "boss");
        request.setAttribute("authTenantId", "tenant_a");
        request.addHeader("X-Tenant-Id", "tenant_b");

        WebhookRequest payload = new WebhookRequest();
        payload.setText("hello");

        ResponseEntity<?> response = controller.wecom(request, payload);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("tenant_conflict", body.get("code"));
        verifyNoInteractions(tenantRepository, integrationWebhookService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void feishuShouldRejectUnauthorizedCaller() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ANALYST");
        request.setAttribute("authUsername", "viewer");
        request.setAttribute("authTenantId", "tenant_default");

        ResponseEntity<?> response = controller.feishu(request, new WebhookRequest());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("forbidden", body.get("code"));
        verifyNoInteractions(tenantRepository, integrationWebhookService);
    }
}
