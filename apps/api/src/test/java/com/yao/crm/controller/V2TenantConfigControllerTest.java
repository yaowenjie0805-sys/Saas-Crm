package com.yao.crm.controller;

import com.yao.crm.dto.request.TenantConfigPatchRequest;
import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V2TenantConfigControllerTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private I18nService i18nService;

    private V2TenantConfigController controller;

    @BeforeEach
    void setUp() {
        controller = new V2TenantConfigController(tenantRepository, i18nService);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void getTenantConfigShouldRejectMissingTenantContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.getTenantConfig(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("bad_request", body.get("code"));
        Map<String, Object> details = (Map<String, Object>) body.get("details");
        assertEquals("tenantId", details.get("field"));
        verifyNoInteractions(tenantRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTenantConfigShouldReturnNotFoundWithTenantNotFoundCode() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authTenantId", "  tenant-a  ");
        when(tenantRepository.findById("tenant-a")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getTenantConfig(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("tenant_not_found", body.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchTenantConfigShouldNormalizeEnumAndChannels() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authTenantId", "  tenant-a  ");

        Tenant tenant = new Tenant();
        tenant.setId("tenant-1");
        tenant.setMarketProfile("CN");
        tenant.setApprovalMode("STRICT");
        tenant.setChannelsJson("[\"WECOM\"]");
        when(tenantRepository.findById("tenant-a")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantConfigPatchRequest payload = new TenantConfigPatchRequest();
        payload.setMarketProfile("  global  ");
        payload.setApprovalMode("  stage_gate  ");
        payload.setChannels(java.util.Arrays.asList(" wecom ", "  ", " dingtalk "));

        ResponseEntity<?> response = controller.patchTenantConfig(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("GLOBAL", body.get("marketProfile"));
        assertEquals("STAGE_GATE", body.get("approvalMode"));
        assertEquals("[\"WECOM\",\"DINGTALK\"]", body.get("channels"));
        verify(tenantRepository).save(tenant);
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchTenantConfigShouldRejectInvalidMarketProfile() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authTenantId", "tenant-1");

        Tenant tenant = new Tenant();
        tenant.setId("tenant-1");
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));

        TenantConfigPatchRequest payload = new TenantConfigPatchRequest();
        payload.setMarketProfile(" apac ");

        ResponseEntity<?> response = controller.patchTenantConfig(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("tenant_market_profile_invalid", body.get("code"));
        verify(tenantRepository, never()).save(any(Tenant.class));
    }
}



