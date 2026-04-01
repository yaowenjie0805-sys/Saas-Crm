package com.yao.crm.controller;

import com.yao.crm.dto.request.V1AcceptInvitationRequest;
import com.yao.crm.dto.request.V1AuthLoginRequest;
import com.yao.crm.dto.request.V1MfaVerifyRequest;
import com.yao.crm.dto.request.SsoLoginRequest;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.repository.UserInvitationRepository;
import com.yao.crm.security.LoginRiskService;
import com.yao.crm.security.MfaChallengeService;
import com.yao.crm.security.MfaService;
import com.yao.crm.security.SessionCookieService;
import com.yao.crm.security.SsoAuthService;
import com.yao.crm.security.SsoIdentity;
import com.yao.crm.security.TokenService;
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

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1AuthControllerTest {

    private UserAccountRepository userAccountRepository;
    private UserInvitationRepository userInvitationRepository;
    private TenantRepository tenantRepository;
    private LoginRiskService loginRiskService;
    private MfaService mfaService;
    private MfaChallengeService mfaChallengeService;
    private SsoAuthService ssoAuthService;
    private V1AuthController controller;

    @BeforeEach
    void setUp() {
        userAccountRepository = mock(UserAccountRepository.class);
        userInvitationRepository = mock(UserInvitationRepository.class);
        tenantRepository = mock(TenantRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenService tokenService = mock(TokenService.class);
        loginRiskService = mock(LoginRiskService.class);
        mfaService = mock(MfaService.class);
        mfaChallengeService = mock(MfaChallengeService.class);
        ssoAuthService = mock(SsoAuthService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        SessionCookieService sessionCookieService = mock(SessionCookieService.class);
        I18nService i18nService = mock(I18nService.class);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));

        controller = new V1AuthController(
                userAccountRepository,
                userInvitationRepository,
                tenantRepository,
                passwordEncoder,
                tokenService,
                loginRiskService,
                mfaService,
                mfaChallengeService,
                ssoAuthService,
                auditLogService,
                sessionCookieService,
                i18nService
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginShouldTrimTenantAndUsernameBeforeLookup() {
        V1AuthLoginRequest payload = new V1AuthLoginRequest();
        payload.setTenantId("  " + TENANT_TEST + "  ");
        payload.setUsername("  admin  ");
        payload.setPassword("bad-password");

        MockHttpServletRequest request = requestWithIp();
        when(loginRiskService.isLocked(TENANT_TEST, "admin", "192.0.2.10")).thenReturn(false);
        when(userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue("admin", TENANT_TEST))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.login(request, payload);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("invalid_credentials", body.get("code"));
        verify(loginRiskService).isLocked(TENANT_TEST, "admin", "192.0.2.10");
        verify(userAccountRepository).findByUsernameAndTenantIdAndEnabledTrue("admin", TENANT_TEST);
        verify(loginRiskService).recordFailure(TENANT_TEST, "admin", "192.0.2.10");
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginShouldReturnValidationErrorWhenTenantBlankAfterTrim() {
        V1AuthLoginRequest payload = new V1AuthLoginRequest();
        payload.setTenantId("   ");
        payload.setUsername("admin");
        payload.setPassword("admin123");

        ResponseEntity<?> response = controller.login(requestWithIp(), payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("validation_error", body.get("code"));
        assertEquals("tenantId", ((Map<String, Object>) body.get("details")).get("field"));
        verifyNoInteractions(loginRiskService, userAccountRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptInvitationShouldReturnValidationErrorWhenTokenBlankAfterTrim() {
        V1AcceptInvitationRequest payload = new V1AcceptInvitationRequest();
        payload.setToken("   ");
        payload.setPassword("invite123");
        payload.setConfirmPassword("invite123");
        payload.setDisplayName("Invite User");

        ResponseEntity<?> response = controller.acceptInvitation(requestWithIp(), payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("validation_error", body.get("code"));
        assertEquals("token", ((Map<String, Object>) body.get("details")).get("field"));
        verifyNoInteractions(userInvitationRepository, userAccountRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void verifyMfaShouldReturnValidationErrorWhenCodeBlankAfterTrim() {
        V1MfaVerifyRequest payload = new V1MfaVerifyRequest();
        payload.setChallengeId("challenge-1");
        payload.setCode("   ");

        ResponseEntity<?> response = controller.verifyMfa(requestWithIp(), payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("validation_error", body.get("code"));
        assertEquals("code", ((Map<String, Object>) body.get("details")).get("field"));
        verifyNoInteractions(mfaService, mfaChallengeService, userAccountRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void oidcCallbackShouldReturnValidationErrorWhenTenantBlankAfterTrim() {
        SsoLoginRequest payload = new SsoLoginRequest();
        payload.setCode("oidc-code");
        payload.setTenantId("   ");
        when(ssoAuthService.isEnabled()).thenReturn(true);
        when(ssoAuthService.resolveIdentity(payload)).thenReturn(new SsoIdentity("oidc_user", "OIDC User"));

        ResponseEntity<?> response = controller.oidcCallback(requestWithIp(), payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("validation_error", body.get("code"));
        assertEquals("tenantId", ((Map<String, Object>) body.get("details")).get("field"));
        verifyNoInteractions(tenantRepository, userAccountRepository);
    }

    private MockHttpServletRequest requestWithIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}

