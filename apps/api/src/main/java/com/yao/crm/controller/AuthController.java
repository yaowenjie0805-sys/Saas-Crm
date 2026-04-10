package com.yao.crm.controller;

import com.yao.crm.dto.request.LoginRequest;
import com.yao.crm.dto.request.RegisterRequest;
import com.yao.crm.dto.request.SsoLoginRequest;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.security.LoginRiskService;
import com.yao.crm.security.MfaService;
import com.yao.crm.security.SessionCookieService;
import com.yao.crm.security.SsoAuthService;
import com.yao.crm.security.SsoIdentity;
import com.yao.crm.security.TokenService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController extends BaseApiController {

    private final UserAccountRepository userAccountRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;
    private final LoginRiskService loginRiskService;
    private final MfaService mfaService;
    private final SsoAuthService ssoAuthService;
    private final SessionCookieService sessionCookieService;
    private final boolean registerInviteOnly;

    public AuthController(UserAccountRepository userAccountRepository,
                          TenantRepository tenantRepository,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          AuditLogService auditLogService,
                          LoginRiskService loginRiskService,
                          MfaService mfaService,
                          SsoAuthService ssoAuthService,
                          SessionCookieService sessionCookieService,
                          @Value("${auth.register.invite-only:true}") boolean registerInviteOnly,
                          I18nService i18nService) {
        super(i18nService);
        this.userAccountRepository = userAccountRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
        this.loginRiskService = loginRiskService;
        this.mfaService = mfaService;
        this.ssoAuthService = ssoAuthService;
        this.sessionCookieService = sessionCookieService;
        this.registerInviteOnly = registerInviteOnly;
    }

    @GetMapping("/auth/sso/config")
    public ResponseEntity<?> ssoConfig() {
        return ResponseEntity.ok(ssoAuthService.config());
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(HttpServletRequest request, @Valid @RequestBody RegisterRequest payload) {
        if (!registerInviteOnly) {
            String tenantId = normalizeOptional(request.getHeader("X-Tenant-Id"));
            if (isBlank(tenantId)) {
                return validationError(request, "tenantId");
            }
            if (!tenantRepository.findById(tenantId).isPresent()) {
                return ResponseEntity.status(404).body(singleMessage(request, "tenant_not_found", "NOT_FOUND", null));
            }

            String username = normalizeOptional(payload.getUsername());
            if (isBlank(username)) {
                return validationError(request, "username");
            }
            if (userAccountRepository.findByUsernameAndTenantId(username, tenantId).isPresent()) {
                return ResponseEntity.status(409).body(singleMessage(request, "username_exists", "CONFLICT", null));
            }

            UserAccount user = new UserAccount();
            user.setId("u_reg_" + Long.toString(System.currentTimeMillis(), 36));
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(payload.getPassword()));
            user.setRole("SALES");
            user.setDisplayName(isBlank(payload.getDisplayName()) ? username : payload.getDisplayName().trim());
            user.setOwnerScope(username);
            user.setEnabled(true);
            user.setTenantId(tenantId);
            user.setDepartment("DEFAULT");
            user.setDataScope("SELF");

            UserAccount created = userAccountRepository.save(user);
            auditLogService.record(created.getUsername(), created.getRole(), "REGISTER", "AUTH", null, "User self-registered", tenantId);

            Map<String, Object> body = buildAuthBody(request, created, false);
            return ResponseEntity.status(201)
                    .header(HttpHeaders.SET_COOKIE, sessionCookieService.buildSessionCookie(String.valueOf(body.get("token"))))
                    .body(body);
        }

        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("activationPath", "/activate");
        details.put("acceptInvitationPath", "/api/v1/auth/invitations/accept");
        return ResponseEntity.status(410).body(errorBody(
                request,
                "register_deprecated_invite_only",
                msg(request, "register_deprecated_invite_only"),
                details
        ));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(HttpServletRequest request, @Valid @RequestBody LoginRequest payload) {
        String username = normalizeOptional(payload.getUsername());
        String password = payload.getPassword();
        String tenantId = normalizeOptional(payload.getTenantId());
        if (isBlank(tenantId)) {
            return validationError(request, "tenantId");
        }
        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();

        if (loginRiskService.isLocked(tenantId, username, ip)) {
            Map<String, Object> details = new HashMap<String, Object>();
            details.put("retryAfterSeconds", loginRiskService.remainingSeconds(tenantId, username, ip));
            return ResponseEntity.status(423).body(singleMessage(request, "login_locked", "LOGIN_LOCKED", details));
        }

        Optional<UserAccount> anyUser = userAccountRepository.findByUsernameAndTenantId(username, tenantId);
        if (!anyUser.isPresent()) {
            loginRiskService.recordFailure(tenantId, username, ip);
            return ResponseEntity.status(401).body(singleMessage(request, "invalid_credentials", "UNAUTHORIZED", null));
        }

        UserAccount user = anyUser.get();
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            return ResponseEntity.status(403).body(singleMessage(request, "account_disabled", "FORBIDDEN", null));
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginRiskService.recordFailure(tenantId, username, ip);
            return ResponseEntity.status(401).body(singleMessage(request, "invalid_credentials", "UNAUTHORIZED", null));
        }

        if (mfaService.requiresMfa(user.getRole())) {
            if (isBlank(payload.getMfaCode())) {
                return ResponseEntity.status(401).body(singleMessage(request, "mfa_required", "UNAUTHORIZED", null));
            }
            if (!mfaService.verify(payload.getMfaCode())) {
                loginRiskService.recordFailure(tenantId, username, ip);
                return ResponseEntity.status(401).body(singleMessage(request, "mfa_invalid", "UNAUTHORIZED", null));
            }
        }

        if (isBlank(user.getTenantId())) {
            return invalidTenantState(request);
        }
        loginRiskService.clear(tenantId, username, ip);
        Map<String, Object> body = buildAuthBody(request, user, false);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieService.buildSessionCookie(String.valueOf(body.get("token"))))
                .body(body);
    }

    @PostMapping("/auth/sso/login")
    public ResponseEntity<?> ssoLogin(HttpServletRequest request, @Valid @RequestBody SsoLoginRequest payload) {
        if (!ssoAuthService.isEnabled()) {
            return ResponseEntity.status(403).body(singleMessage(request, "sso_disabled", "FORBIDDEN", null));
        }

        SsoIdentity identity = ssoAuthService.resolveIdentity(payload);
        if (identity == null) {
            String key = "oidc".equalsIgnoreCase(ssoAuthService.mode())
                    ? "sso_oidc_exchange_failed"
                    : "sso_invalid_code";
            return ResponseEntity.status(401).body(singleMessage(request, key, "UNAUTHORIZED", null));
        }

        String username = identity.getUsername();
        String tenantId = normalizeOptional(payload.getTenantId());
        if (isBlank(tenantId)) {
            return validationError(request, "tenantId");
        }
        if (!tenantRepository.findById(tenantId).isPresent()) {
            return ResponseEntity.status(404).body(singleMessage(request, "tenant_not_found", "NOT_FOUND", null));
        }
        Optional<UserAccount> optional = userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue(username, tenantId);
        UserAccount user;
        if (optional.isPresent()) {
            user = optional.get();
        } else {
            if (!ssoAuthService.allowAutoProvision()) {
                return ResponseEntity.status(404).body(singleMessage(request, "user_not_found", "NOT_FOUND", null));
            }
            user = new UserAccount();
            user.setId("u_sso_" + Long.toString(System.currentTimeMillis(), 36));
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("sso-user"));
            user.setRole(ssoAuthService.defaultRole());
            user.setDisplayName(identity.getDisplayName());
            user.setOwnerScope("SALES".equals(user.getRole()) ? username : "");
            user.setEnabled(true);
            user.setTenantId(tenantId);
            user.setDepartment("DEFAULT");
            user.setDataScope("SELF");
            user = userAccountRepository.save(user);
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            return ResponseEntity.status(403).body(singleMessage(request, "forbidden", "FORBIDDEN", null));
        }
        if (isBlank(user.getTenantId())) {
            return invalidTenantState(request);
        }

        auditLogService.record(user.getUsername(), user.getRole(), "LOGIN_SSO", "AUTH", null, "User logged in via " + ssoAuthService.providerName(), user.getTenantId());
        Map<String, Object> body = buildAuthBody(request, user, true);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieService.buildSessionCookie(String.valueOf(body.get("token"))))
                .body(body);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String username = String.valueOf(request.getAttribute("authUsername"));
        String role = String.valueOf(request.getAttribute("authRole"));
        String tenantId = String.valueOf(request.getAttribute("authTenantId"));
        auditLogService.record(username, role, "LOGOUT", "AUTH", null, "User logged out", tenantId);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieService.buildClearCookie())
                .body(successByKey(request, "logout_success", null));
    }

    @GetMapping("/auth/session")
    public ResponseEntity<?> session(HttpServletRequest request) {
        String username = currentUser(request);
        String tenantId = currentTenant(request);
        Optional<UserAccount> optional = userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue(username, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(401).body(singleMessage(request, "invalid_or_expired", "UNAUTHORIZED", null));
        }
        UserAccount user = optional.get();
        if (isBlank(user.getTenantId())) {
            return invalidTenantState(request);
        }
        return ResponseEntity.ok(buildAuthBody(request, user, true));
    }

    private Map<String, Object> buildAuthBody(HttpServletRequest request, UserAccount user, boolean sso) {
        String ownerScope = isBlank(user.getOwnerScope()) ? user.getUsername() : user.getOwnerScope();
        String tenantId = normalizeOptional(user.getTenantId());
        String token = tokenService.createToken(user.getUsername(), user.getRole(), ownerScope, tenantId, true);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("token", token);
        body.put("username", user.getUsername());
        body.put("displayName", user.getDisplayName());
        body.put("role", user.getRole());
        body.put("ownerScope", ownerScope);
        body.put("tenantId", tenantId);
        body.put("department", user.getDepartment());
        body.put("dataScope", user.getDataScope());
        body.put("mfaEnabled", mfaService.isEnabled());
        body.put("sso", sso);
        body.put("requestId", traceId(request));
        return body;
    }

    private Map<String, Object> singleMessage(HttpServletRequest request, String messageKey, String fallbackCode, Map<String, Object> details) {
        String code = legacyCode(messageKey, fallbackCode);
        return legacyErrorBody(request, code, msg(request, messageKey), details);
    }

    private ResponseEntity<?> validationError(HttpServletRequest request, String field) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("field", field);
        return ResponseEntity.badRequest().body(singleMessage(request, "validation_error", "BAD_REQUEST", details));
    }

    private ResponseEntity<?> invalidTenantState(HttpServletRequest request) {
        return ResponseEntity.status(401).body(singleMessage(request, "invalid_or_expired", "UNAUTHORIZED", null));
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? "" : value.trim();
    }
}



