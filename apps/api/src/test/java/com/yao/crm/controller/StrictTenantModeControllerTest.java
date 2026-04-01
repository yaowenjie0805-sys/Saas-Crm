package com.yao.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.security.AuthInterceptor;
import com.yao.crm.security.AuthPrincipal;
import com.yao.crm.security.SessionCookieService;
import com.yao.crm.security.TenantRequirementMode;
import com.yao.crm.security.TokenService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StrictTenantModeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TokenService tokenService;

    @Mock
    private I18nService i18nService;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuditLogService auditLogService;

    private boolean originalRejectMissingTenant;

    @BeforeEach
    void setUp() {
        originalRejectMissingTenant = TenantRequirementMode.isRejectMissingTenant();
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        AuthInterceptor interceptor = new AuthInterceptor(
                tokenService,
                i18nService,
                new ObjectMapper(),
                tenantRepository,
                auditLogService,
                new SessionCookieService("CRM_SESSION", "/", "Lax", false, 86400L)
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DashboardProbeController())
                .addInterceptors(interceptor)
                .build();
    }

    @AfterEach
    void restoreTenantRequirementMode() {
        new TenantRequirementMode(originalRejectMissingTenant);
    }

    @Test
    void shouldRejectMissingTenantWhenStrictModeEnabled() throws Exception {
        new TenantRequirementMode(true);
        when(tokenService.verify("token"))
                .thenReturn(new AuthPrincipal("admin", "ADMIN", "admin", " ", true));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("tenant_required"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @RestController
    private static class DashboardProbeController {

        @GetMapping("/api/dashboard")
        ResponseEntity<Map<String, Object>> dashboard() {
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        }
    }
}
