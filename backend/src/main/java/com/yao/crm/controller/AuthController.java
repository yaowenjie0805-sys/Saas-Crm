package com.yao.crm.controller;

import com.yao.crm.dto.request.LoginRequest;
import com.yao.crm.dto.request.RegisterRequest;
import com.yao.crm.dto.request.SsoLoginRequest;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.security.LoginRiskService;
import com.yao.crm.security.MfaService;
import com.yao.crm.security.SsoAuthService;
import com.yao.crm.security.SsoIdentity;
import com.yao.crm.security.TokenService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController extends BaseApiController {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;
    private final LoginRiskService loginRiskService;
    private final MfaService mfaService;
    private final SsoAuthService ssoAuthService;

    public AuthController(UserAccountRepository userAccountRepository,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          AuditLogService auditLogService,
                          LoginRiskService loginRiskService,
                          MfaService mfaService,
                          SsoAuthService ssoAuthService,
                          I18nService i18nService) {
        super(i18nService);
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
        this.loginRiskService = loginRiskService;
        this.mfaService = mfaService;
        this.ssoAuthService = ssoAuthService;
    }

    @GetMapping("/auth/sso/config")
    public ResponseEntity<?> ssoConfig() {
        return ResponseEntity.ok(ssoAuthService.config());
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(HttpServletRequest request, @Valid @RequestBody RegisterRequest payload) {
        String username = payload.getUsername() == null ? "" : payload.getUsername().trim().toLowerCase(Locale.ROOT);
        if (isBlank(username)) {
            return ResponseEntity.badRequest().body(singleMessage(request, "register_username_required", "BAD_REQUEST", null));
        }

        if (userAccountRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(409).body(singleMessage(request, "username_exists", "CONFLICT", null));
        }

        UserAccount user = new UserAccount();
        user.setId("u_" + Long.toString(System.currentTimeMillis(), 36));
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(payload.getPassword()));
        user.setRole("SALES");
        user.setDisplayName(isBlank(payload.getDisplayName()) ? username : payload.getDisplayName().trim());
        user.setOwnerScope(username);
        user.setEnabled(true);
        user.setTenantId("tenant_default");
        user.setDepartment("DEFAULT");
        user.setDataScope("SELF");
        user = userAccountRepository.save(user);

        auditLogService.record(user.getUsername(), user.getRole(), "REGISTER", "AUTH", null, "User self-registered", user.getTenantId());
        return ResponseEntity.status(201).body(buildAuthBody(request, user, false));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(HttpServletRequest request, @Valid @RequestBody LoginRequest payload) {
        String username = payload.getUsername();
        String password = payload.getPassword();
        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();

        if (loginRiskService.isLocked(username, ip)) {
            Map<String, Object> details = new HashMap<String, Object>();
            details.put("retryAfterSeconds", loginRiskService.remainingSeconds(username, ip));
            return ResponseEntity.status(423).body(singleMessage(request, "login_locked", "LOGIN_LOCKED", details));
        }

        String tenantId = payload.getTenantId() == null ? "" : payload.getTenantId().trim();
        Optional<UserAccount> anyUser = isBlank(tenantId)
                ? userAccountRepository.findByUsername(username)
                : userAccountRepository.findByUsernameAndTenantId(username, tenantId);
        if (!anyUser.isPresent()) {
            loginRiskService.recordFailure(username, ip);
            return ResponseEntity.status(401).body(singleMessage(request, "invalid_credentials", "UNAUTHORIZED", null));
        }

        UserAccount user = anyUser.get();
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            return ResponseEntity.status(403).body(singleMessage(request, "account_disabled", "FORBIDDEN", null));
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginRiskService.recordFailure(username, ip);
            return ResponseEntity.status(401).body(singleMessage(request, "invalid_credentials", "UNAUTHORIZED", null));
        }

        if (mfaService.requiresMfa(user.getRole())) {
            if (isBlank(payload.getMfaCode())) {
                return ResponseEntity.status(401).body(singleMessage(request, "mfa_required", "UNAUTHORIZED", null));
            }
            if (!mfaService.verify(payload.getMfaCode())) {
                loginRiskService.recordFailure(username, ip);
                return ResponseEntity.status(401).body(singleMessage(request, "mfa_invalid", "UNAUTHORIZED", null));
            }
        }

        loginRiskService.clear(username, ip);
        return ResponseEntity.ok(buildAuthBody(request, user, false));
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
        String tenantId = isBlank(payload.getTenantId()) ? "tenant_default" : payload.getTenantId().trim();
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

        auditLogService.record(user.getUsername(), user.getRole(), "LOGIN_SSO", "AUTH", null, "User logged in via " + ssoAuthService.providerName(), user.getTenantId());
        return ResponseEntity.ok(buildAuthBody(request, user, true));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String username = String.valueOf(request.getAttribute("authUsername"));
        String role = String.valueOf(request.getAttribute("authRole"));
        String tenantId = String.valueOf(request.getAttribute("authTenantId"));
        auditLogService.record(username, role, "LOGOUT", "AUTH", null, "User logged out", tenantId);
        return ResponseEntity.ok(successByKey(request, "logout_success", null));
    }

    private Map<String, Object> buildAuthBody(HttpServletRequest request, UserAccount user, boolean sso) {
        String ownerScope = isBlank(user.getOwnerScope()) ? user.getUsername() : user.getOwnerScope();
        String tenantId = isBlank(user.getTenantId()) ? "tenant_default" : user.getTenantId();
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
}



