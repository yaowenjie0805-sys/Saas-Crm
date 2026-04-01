package com.yao.crm.security;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {
    private static final String TENANT_MISSING = TENANT_TEST + "-missing";

    @Mock
    private TokenService tokenService;

    @Mock
    private I18nService i18nService;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuditLogService auditLogService;

    private AuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        TenantRequirementMode.setRejectMissingTenantForTesting(false);
        lenient().when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        interceptor = new AuthInterceptor(
                tokenService,
                i18nService,
                new ObjectMapper(),
                tenantRepository,
                auditLogService,
                new SessionCookieService("CRM_SESSION", "/", "Lax", false, 86400L)
        );
    }

    @Test
    void shouldRejectWhenMissingTenantAndStrictModeEnabled() throws Exception {
        TenantRequirementMode.setRejectMissingTenantForTesting(true);
        AuthPrincipal principal = new AuthPrincipal("alice", "ADMIN", "alice", " ", true);
        when(tokenService.verify("token")).thenReturn(principal);

        MockHttpServletRequest request = buildRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldCacheTenantDateFormatForRepeatedRequests() throws Exception {
        AuthPrincipal principal = new AuthPrincipal("alice", "ADMIN", "alice", TENANT_TEST, true);
        when(tokenService.verify("token")).thenReturn(principal);

        Tenant tenant = new Tenant();
        tenant.setId(TENANT_TEST);
        tenant.setDateFormat("dd/MM/yyyy");
        when(tenantRepository.findById(TENANT_TEST)).thenReturn(Optional.of(tenant));

        MockHttpServletRequest firstRequest = buildRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(firstRequest, firstResponse, new Object()));
        assertEquals("dd/MM/yyyy", firstRequest.getAttribute("authTenantDateFormat"));

        MockHttpServletRequest secondRequest = buildRequest();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(secondRequest, secondResponse, new Object()));
        assertEquals("dd/MM/yyyy", secondRequest.getAttribute("authTenantDateFormat"));

        verify(tenantRepository).findById(TENANT_TEST);
    }

    @Test
    void shouldFallbackToDefaultDateFormatWhenTenantIsMissing() throws Exception {
        AuthPrincipal principal = new AuthPrincipal("alice", "ADMIN", "alice", TENANT_MISSING, true);
        when(tokenService.verify("token")).thenReturn(principal);
        when(tenantRepository.findById(TENANT_MISSING)).thenReturn(Optional.empty());

        MockHttpServletRequest request = buildRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals("yyyy-MM-dd", request.getAttribute("authTenantDateFormat"));

        verify(tenantRepository).findById(TENANT_MISSING);
    }

    @Test
    void shouldReuseCachedPrincipalAttributesWithoutVerifyingTokenAgain() throws Exception {
        AuthPrincipal principal = new AuthPrincipal("alice", "ADMIN", "alice", TENANT_TEST, true);

        Tenant tenant = new Tenant();
        tenant.setId(TENANT_TEST);
        tenant.setDateFormat("dd/MM/yyyy");
        when(tenantRepository.findById(TENANT_TEST)).thenReturn(Optional.of(tenant));

        MockHttpServletRequest request = buildRequest();
        request.setAttribute("authPrincipal", principal);
        request.setAttribute("authUsername", "alice");
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authOwnerScope", "alice");
        request.setAttribute("authTenantId", TENANT_TEST);
        request.setAttribute("authMfaVerified", Boolean.TRUE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals("dd/MM/yyyy", request.getAttribute("authTenantDateFormat"));

        verify(tokenService, never()).verify("token");
        verify(tenantRepository).findById(TENANT_TEST);
    }

    @Test
    void shouldRejectBlankBearerTokenAsMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/dashboard");
        request.addHeader("Authorization", "Bearer    ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        verify(tokenService, never()).verify("token");
    }

    @Test
    void shouldNormalizeTenantIdBeforeHeaderComparisonAndTenantCacheLookup() throws Exception {
        AuthPrincipal principal = new AuthPrincipal("alice", "ADMIN", "alice", " tenant-1 ", true);
        when(tokenService.verify("token")).thenReturn(principal);

        Tenant tenant = new Tenant();
        tenant.setId(TENANT_TEST);
        tenant.setDateFormat("dd/MM/yyyy");
        when(tenantRepository.findById(TENANT_TEST)).thenReturn(Optional.of(tenant));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/customers");
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("X-Tenant-Id", TENANT_TEST);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(TENANT_TEST, request.getAttribute("authTenantId"));
        assertEquals("dd/MM/yyyy", request.getAttribute("authTenantDateFormat"));
        verify(tenantRepository).findById(TENANT_TEST);
    }

    @Test
    void shouldTreatTokenVerificationExceptionAsInvalidToken() throws Exception {
        when(tokenService.verify("token")).thenThrow(new IllegalStateException("boom"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/dashboard");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldRejectTenantScopedPathWhenPrincipalTenantIsBlank() throws Exception {
        AuthPrincipal principal = new AuthPrincipal("alice", "ADMIN", "alice", "  ", true);
        when(tokenService.verify("token")).thenReturn(principal);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/customers");
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("X-Tenant-Id", TENANT_TEST);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldUseUnknownTenantInAuditWhenCrossTenantForbiddenAndPrincipalTenantIsBlank() throws Exception {
        AuthPrincipal principal = new AuthPrincipal("alice", "ADMIN", "alice", "  ", true);
        when(tokenService.verify("token")).thenReturn(principal);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/dashboard");
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("X-Tenant-Id", TENANT_TEST);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
        verify(auditLogService).record(
                eq("alice"),
                eq("ADMIN"),
                eq("TENANT_FORBIDDEN"),
                eq("AUTHZ"),
                eq("/api/dashboard"),
                anyString(),
                eq("unknown")
        );
    }

    @Test
    void shouldAllowSessionEndpointWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/auth/session");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(tokenService, never()).verify(anyString());
    }

    private MockHttpServletRequest buildRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/dashboard");
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}

