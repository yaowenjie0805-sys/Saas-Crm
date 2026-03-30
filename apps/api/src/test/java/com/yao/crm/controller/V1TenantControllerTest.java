package com.yao.crm.controller;

import com.yao.crm.dto.request.V1TenantUpsertRequest;
import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1TenantControllerTest {

    private TenantRepository tenantRepository;
    private V1TenantController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        I18nService i18nService = mock(I18nService.class);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));

        controller = new V1TenantController(
                tenantRepository,
                userAccountRepository,
                passwordEncoder,
                auditLogService,
                "admin123",
                i18nService
        );

        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authUsername", "alice");
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateTenantShouldRejectBlankPathId() {
        V1TenantUpsertRequest payload = validPayload();

        ResponseEntity<?> response = controller.updateTenant(request, "   ", payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("bad_request", body.get("code"));
        verifyNoInteractions(tenantRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateTenantShouldTrimPathIdBeforeLookup() {
        V1TenantUpsertRequest payload = validPayload();
        Tenant tenant = existingTenant("tn_1");
        when(tenantRepository.findById("tn_1")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.updateTenant(request, "  tn_1  ", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tenantRepository).findById("tn_1");
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("tn_1", body.get("id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateTenantShouldRejectInvalidApprovalModeAfterTrim() {
        V1TenantUpsertRequest payload = validPayload();
        payload.setApprovalMode("  stage-gate  ");
        Tenant tenant = existingTenant("tn_1");
        when(tenantRepository.findById("tn_1")).thenReturn(Optional.of(tenant));

        ResponseEntity<?> response = controller.updateTenant(request, "tn_1", payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("tenant_approval_mode_invalid", body.get("code"));
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    private V1TenantUpsertRequest validPayload() {
        V1TenantUpsertRequest payload = new V1TenantUpsertRequest();
        payload.setName("Tenant A");
        payload.setQuotaUsers(50);
        payload.setStatus("active");
        payload.setTimezone("Asia/Shanghai");
        payload.setCurrency("cny");
        payload.setDateFormat("yyyy-MM-dd");
        payload.setMarketProfile("cn");
        payload.setTaxRule("vat_cn");
        payload.setApprovalMode("strict");
        payload.setChannels("[\"WECOM\"]");
        payload.setDataResidency("cn");
        payload.setMaskLevel("standard");
        return payload;
    }

    private Tenant existingTenant(String id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName("Old Tenant");
        tenant.setStatus("ACTIVE");
        tenant.setQuotaUsers(20);
        tenant.setTimezone("Asia/Shanghai");
        tenant.setCurrency("CNY");
        tenant.setDateFormat("yyyy-MM-dd");
        tenant.setMarketProfile("CN");
        tenant.setTaxRule("VAT_CN");
        tenant.setApprovalMode("STRICT");
        tenant.setChannelsJson("[\"WECOM\"]");
        tenant.setDataResidency("CN");
        tenant.setMaskLevel("STANDARD");
        return tenant;
    }
}
