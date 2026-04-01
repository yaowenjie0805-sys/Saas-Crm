package com.yao.crm.controller;

import com.yao.crm.dto.request.LoginRequest;
import com.yao.crm.dto.request.SsoLoginRequest;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.security.LoginRiskService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private TenantRepository tenantRepository;
    private UserAccountRepository userAccountRepository;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private LoginRiskService loginRiskService;
    private MfaService mfaService;
    private SsoAuthService ssoAuthService;
    private AuditLogService auditLogService;
    private SessionCookieService sessionCookieService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(TokenService.class);
        auditLogService = mock(AuditLogService.class);
        loginRiskService = mock(LoginRiskService.class);
        mfaService = mock(MfaService.class);
        ssoAuthService = mock(SsoAuthService.class);
        sessionCookieService = mock(SessionCookieService.class);
        I18nService i18nService = mock(I18nService.class);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));

        controller = new AuthController(
                userAccountRepository,
                tenantRepository,
                passwordEncoder,
                tokenService,
                auditLogService,
                loginRiskService,
                mfaService,
                ssoAuthService,
                sessionCookieService,
                i18nService
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginShouldReturnValidationErrorWhenTenantBlank() {
        LoginRequest payload = new LoginRequest();
        payload.setTenantId("   ");
        payload.setUsername("  alice  ");
        payload.setPassword("bad");

        MockHttpServletRequest request = requestWithIp();

        ResponseEntity<?> response = controller.login(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("VALIDATION_ERROR", body.get("code"));
        assertEquals("tenantId", ((Map<String, Object>) body.get("details")).get("field"));
        verifyNoInteractions(loginRiskService, userAccountRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginShouldReturnLockedWhenTenantScopedRiskHits() {
        LoginRequest payload = new LoginRequest();
        payload.setTenantId(TENANT_TEST);
        payload.setUsername("alice");
        payload.setPassword("irrelevant");

        MockHttpServletRequest request = requestWithIp();
        when(loginRiskService.isLocked(TENANT_TEST, "alice", "192.0.2.10")).thenReturn(true);
        when(loginRiskService.remainingSeconds(TENANT_TEST, "alice", "192.0.2.10")).thenReturn(42L);

        ResponseEntity<?> response = controller.login(request, payload);

        assertEquals(HttpStatus.LOCKED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("LOGIN_LOCKED", body.get("code"));
        Map<String, Object> details = (Map<String, Object>) body.get("details");
        assertEquals(42L, details.get("retryAfterSeconds"));
        verifyNoInteractions(userAccountRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void ssoLoginShouldReturnValidationErrorWhenTenantBlank() {
        SsoLoginRequest payload = new SsoLoginRequest();
        payload.setCode("code");
        payload.setTenantId("   ");
        when(ssoAuthService.isEnabled()).thenReturn(true);
        when(ssoAuthService.resolveIdentity(payload)).thenReturn(new SsoIdentity("alice", "Alice"));

        ResponseEntity<?> response = controller.ssoLogin(requestWithIp(), payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("VALIDATION_ERROR", body.get("code"));
        assertEquals("tenantId", ((Map<String, Object>) body.get("details")).get("field"));
        verifyNoInteractions(tenantRepository, userAccountRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginShouldRejectWhenUserTenantIsBlank() {
        LoginRequest payload = new LoginRequest();
        payload.setTenantId(TENANT_TEST);
        payload.setUsername("alice");
        payload.setPassword("password-1");

        UserAccount user = new UserAccount();
        user.setUsername("alice");
        user.setPassword("encoded-password");
        user.setRole("SALES");
        user.setEnabled(true);
        user.setTenantId("   ");

        when(loginRiskService.isLocked(TENANT_TEST, "alice", "192.0.2.10")).thenReturn(false);
        when(userAccountRepository.findByUsernameAndTenantId("alice", TENANT_TEST)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password-1", "encoded-password")).thenReturn(true);

        ResponseEntity<?> response = controller.login(requestWithIp(), payload);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("INVALID_OR_EXPIRED", body.get("code"));
        verify(tokenService, never()).createToken(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    private MockHttpServletRequest requestWithIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}

