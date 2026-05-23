package com.yao.crm.controller;

import com.yao.crm.dto.request.UpdateTenantFeatureFlagRequest;
import com.yao.crm.dto.request.UpdateTenantSubscriptionRequest;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.BillingService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V1BillingControllerTest {

    private BillingService billingService;
    private AuditLogService auditLogService;
    private V1BillingController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        billingService = mock(BillingService.class);
        auditLogService = mock(AuditLogService.class);
        I18nService i18nService = mock(I18nService.class);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        controller = new V1BillingController(billingService, auditLogService, i18nService);
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authUsername", "alice");
        request.setAttribute("authTenantId", "tenant-a");
    }

    @Test
    @SuppressWarnings("unchecked")
    void plansShouldAllowReadRoles() {
        request.setAttribute("authRole", "ANALYST");
        when(billingService.plans()).thenReturn(List.of(Map.of("code", "FREE")));

        ResponseEntity<?> response = controller.plans(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("billing_plans_loaded", body.get("code"));
        assertEquals(1, ((List<?>) body.get("items")).size());
    }

    @Test
    void subscriptionShouldRejectCrossTenantAccess() {
        ResponseEntity<?> response = controller.subscription(request, "tenant-b");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(billingService, never()).subscription(anyString());
    }

    @Test
    void updateSubscriptionShouldAuditSuccessfulChange() {
        UpdateTenantSubscriptionRequest payload = new UpdateTenantSubscriptionRequest();
        payload.setPlanCode("BUSINESS");
        payload.setStatus("ACTIVE");
        Map<String, Object> serviceBody = new LinkedHashMap<String, Object>();
        serviceBody.put("tenantId", "tenant-a");
        serviceBody.put("planCode", "BUSINESS");
        when(billingService.updateSubscription(anyString(), anyString(), anyString(), any(), any())).thenReturn(serviceBody);

        ResponseEntity<?> response = controller.updateSubscription(request, "tenant-a", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditLogService).record("alice", "ADMIN", "UPDATE", "BILLING_SUBSCRIPTION", "tenant-a", "Updated tenant subscription", "tenant-a");
    }

    @Test
    void updateFeatureShouldRequireEnabledFlag() {
        UpdateTenantFeatureFlagRequest payload = new UpdateTenantFeatureFlagRequest();

        ResponseEntity<?> response = controller.updateFeature(request, "tenant-a", "sso", payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(billingService, never()).updateFeature(anyString(), anyString(), any(Boolean.class), anyString());
    }
}
