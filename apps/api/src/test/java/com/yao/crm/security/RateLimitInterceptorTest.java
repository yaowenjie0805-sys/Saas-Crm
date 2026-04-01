package com.yao.crm.security;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {
    private static final String TENANT_B = TENANT_TEST + "-b";

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private I18nService i18nService;

    @Mock
    private TokenService tokenService;

    @Mock
    private SessionCookieService sessionCookieService;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        lenient().when(sessionCookieService.cookieName()).thenReturn("CRM_SESSION");
        interceptor = new RateLimitInterceptor(
                rateLimitService,
                i18nService,
                new ObjectMapper(),
                tokenService,
                sessionCookieService,
                30,
                120,
                20,
                40
        );
    }

    @Test
    @DisplayName("shouldUseAuthAttributesForBearerTokenRequestsWithoutTokenVerification")
    void shouldUseTenantUserRouteKeyForBearerTokenRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/approval/requests/123");
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("Authorization", "Bearer token-123");
        request.addHeader("X-Tenant-Id", TENANT_TEST);
        request.setAttribute("authTenantId", TENANT_TEST);
        request.setAttribute("authUsername", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitService.allow(anyString(), anyInt())).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(rateLimitService).allow(keyCaptor.capture(), limitCaptor.capture());
        assertEquals(TENANT_TEST + "|alice|/api/v1/approval/requests/{id}", keyCaptor.getValue());
        assertEquals(Integer.valueOf(120), limitCaptor.getValue());
        verify(tokenService, never()).verify(anyString());
    }

    @Test
    @DisplayName("shouldUseAuthAttributesForSessionCookieRequestsWithoutTokenVerification")
    void shouldUseTenantUserRouteKeyForSessionCookieRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/export-jobs/42");
        request.setRemoteAddr("10.0.0.6");
        request.setCookies(new Cookie("CRM_SESSION", "cookie-token"));
        request.setAttribute("authTenantId", TENANT_B);
        request.setAttribute("authUsername", "bob");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitService.allow(anyString(), anyInt())).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(rateLimitService).allow(keyCaptor.capture(), limitCaptor.capture());
        assertEquals(TENANT_B + "|bob|/api/v1/export-jobs/{id}", keyCaptor.getValue());
        assertEquals(Integer.valueOf(40), limitCaptor.getValue());
        verify(tokenService, never()).verify(anyString());
    }

    @Test
    @DisplayName("shouldNormalizeUuidPathSegmentsInRouteKey")
    void shouldNormalizeUuidPathSegmentsInRouteKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/approval/requests/550e8400-e29b-41d4-a716-446655440000");
        request.setRemoteAddr("10.0.0.7");
        request.addHeader("Authorization", "Bearer token-123");
        request.addHeader("X-Tenant-Id", TENANT_TEST);
        request.setAttribute("authTenantId", TENANT_TEST);
        request.setAttribute("authUsername", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitService.allow(anyString(), anyInt())).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(rateLimitService).allow(keyCaptor.capture(), limitCaptor.capture());
        assertEquals(TENANT_TEST + "|alice|/api/v1/approval/requests/{uuid}", keyCaptor.getValue());
        assertEquals(Integer.valueOf(120), limitCaptor.getValue());
        verify(tokenService, never()).verify(anyString());
    }

    @Test
    @DisplayName("shouldFallbackToBearerTokenVerificationWhenAuthAttributesAreMissing")
    void shouldFallbackToBearerTokenVerificationWhenAuthAttributesAreMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/approval/requests/123");
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("Authorization", "Bearer token-123");
        request.addHeader("X-Tenant-Id", TENANT_TEST);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenService.verify("token-123")).thenReturn(new AuthPrincipal("alice", "ADMIN", "alice", TENANT_TEST, true));
        when(rateLimitService.allow(anyString(), anyInt())).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(rateLimitService).allow(keyCaptor.capture(), limitCaptor.capture());
        assertEquals(TENANT_TEST + "|alice|/api/v1/approval/requests/{id}", keyCaptor.getValue());
        assertEquals(Integer.valueOf(120), limitCaptor.getValue());
        verify(tokenService).verify("token-123");
    }

    @Test
    @DisplayName("shouldCacheResolvedPrincipalOnTheRequestForDownstreamReuse")
    void shouldCacheResolvedPrincipalOnTheRequestForDownstreamReuse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/approval/requests/123");
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("Authorization", "Bearer token-123");
        request.addHeader("X-Tenant-Id", TENANT_TEST);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthPrincipal principal = new AuthPrincipal("alice", "ADMIN", "alice", TENANT_TEST, true);
        when(tokenService.verify("token-123")).thenReturn(principal);
        when(rateLimitService.allow(anyString(), anyInt())).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        assertNotNull(request.getAttribute("authPrincipal"));
        assertEquals(principal, request.getAttribute("authPrincipal"));
        assertEquals("alice", request.getAttribute("authUsername"));
        assertEquals("ADMIN", request.getAttribute("authRole"));
        assertEquals("alice", request.getAttribute("authOwnerScope"));
        assertEquals(TENANT_TEST, request.getAttribute("authTenantId"));
        assertEquals(Boolean.TRUE, request.getAttribute("authMfaVerified"));
        verify(tokenService).verify("token-123");
    }

    @Test
    @DisplayName("shouldFallbackToIpAndRouteWhenUnauthenticated")
    void shouldFallbackToIpAndRouteWhenUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.15");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitService.allow(anyString(), anyInt())).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(rateLimitService).allow(keyCaptor.capture(), limitCaptor.capture());
        assertEquals("192.0.2.15|/api/v1/auth/login", keyCaptor.getValue());
        assertEquals(Integer.valueOf(30), limitCaptor.getValue());
        verify(tokenService, never()).verify(anyString());
    }

    @Test
    @DisplayName("shouldUseUsernameIpRouteFallbackWhenAuthTenantIsMissing")
    void shouldUseUsernameIpRouteFallbackWhenAuthTenantIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/approval/requests/123");
        request.setRemoteAddr("10.0.0.9");
        request.addHeader("Authorization", "Bearer token-123");
        request.setAttribute("authUsername", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitService.allow(anyString(), anyInt())).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(rateLimitService).allow(keyCaptor.capture(), limitCaptor.capture());
        assertEquals("user:alice|10.0.0.9|/api/v1/approval/requests/{id}", keyCaptor.getValue());
        assertEquals(Integer.valueOf(120), limitCaptor.getValue());
        verify(tokenService, never()).verify(anyString());
    }

    @Test
    @DisplayName("shouldUseUsernameIpRouteFallbackWhenPrincipalTenantIsMissing")
    void shouldUseUsernameIpRouteFallbackWhenPrincipalTenantIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/approval/requests/123");
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("Authorization", "Bearer token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenService.verify("token-123")).thenReturn(new AuthPrincipal("alice", "ADMIN", "alice", null, true));
        when(rateLimitService.allow(anyString(), anyInt())).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(rateLimitService).allow(keyCaptor.capture(), limitCaptor.capture());
        assertEquals("user:alice|10.0.0.10|/api/v1/approval/requests/{id}", keyCaptor.getValue());
        assertEquals(Integer.valueOf(120), limitCaptor.getValue());
        verify(tokenService).verify("token-123");
    }

    @Test
    @DisplayName("shouldCollapseDifferentNumericIdsToTheSameRouteKey")
    void shouldCollapseDifferentNumericIdsToTheSameRouteKey() throws Exception {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/api/v1/approval/requests/123");
        firstRequest.setRemoteAddr("10.0.0.5");
        firstRequest.addHeader("Authorization", "Bearer token-123");
        firstRequest.addHeader("X-Tenant-Id", TENANT_TEST);

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("GET", "/api/v1/approval/requests/456");
        secondRequest.setRemoteAddr("10.0.0.5");
        secondRequest.addHeader("Authorization", "Bearer token-123");
        secondRequest.addHeader("X-Tenant-Id", TENANT_TEST);

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        when(tokenService.verify("token-123")).thenReturn(new AuthPrincipal("alice", "ADMIN", "alice", TENANT_TEST, true));
        when(rateLimitService.allow(anyString(), anyInt())).thenReturn(true);

        assertTrue(interceptor.preHandle(firstRequest, firstResponse, new Object()));
        assertTrue(interceptor.preHandle(secondRequest, secondResponse, new Object()));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(rateLimitService, times(2)).allow(keyCaptor.capture(), limitCaptor.capture());
        assertEquals(TENANT_TEST + "|alice|/api/v1/approval/requests/{id}", keyCaptor.getAllValues().get(0));
        assertEquals(TENANT_TEST + "|alice|/api/v1/approval/requests/{id}", keyCaptor.getAllValues().get(1));
        assertEquals(Integer.valueOf(120), limitCaptor.getAllValues().get(0));
        assertEquals(Integer.valueOf(120), limitCaptor.getAllValues().get(1));
        verify(tokenService, times(2)).verify("token-123");
    }
}

