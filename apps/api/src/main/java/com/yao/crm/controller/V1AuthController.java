package com.yao.crm.controller;

import com.yao.crm.dto.request.V1AuthLoginRequest;
import com.yao.crm.dto.request.V1MfaVerifyRequest;
import com.yao.crm.dto.request.V1AcceptInvitationRequest;
import com.yao.crm.dto.request.SsoLoginRequest;
import com.yao.crm.entity.UserInvitation;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.UserInvitationRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.security.*;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Optional;

@Tag(name = "Authentication", description = "Login, logout and token management")
@RestController
@RequestMapping("/api/v1/auth")
public class V1AuthController extends BaseApiController {

    private final UserAccountRepository userAccountRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginRiskService loginRiskService;
    private final MfaService mfaService;
    private final MfaChallengeService mfaChallengeService;
    private final SsoAuthService ssoAuthService;
    private final AuditLogService auditLogService;
    private final SessionCookieService sessionCookieService;

    public V1AuthController(UserAccountRepository userAccountRepository,
                            UserInvitationRepository userInvitationRepository,
                            TenantRepository tenantRepository,
                            PasswordEncoder passwordEncoder,
                            TokenService tokenService,
                            LoginRiskService loginRiskService,
                            MfaService mfaService,
                            MfaChallengeService mfaChallengeService,
                            SsoAuthService ssoAuthService,
                            AuditLogService auditLogService,
                            SessionCookieService sessionCookieService,
                            I18nService i18nService) {
        super(i18nService);
        this.userAccountRepository = userAccountRepository;
        this.userInvitationRepository = userInvitationRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginRiskService = loginRiskService;
        this.mfaService = mfaService;
        this.mfaChallengeService = mfaChallengeService;
        this.ssoAuthService = ssoAuthService;
        this.auditLogService = auditLogService;
        this.sessionCookieService = sessionCookieService;
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<?> acceptInvitation(HttpServletRequest request, @Valid @RequestBody V1AcceptInvitationRequest payload) {
        String invitationToken = normalizeRequiredValue(payload.getToken());
        if (invitationToken == null) {
            return validationError(request, "token");
        }
        if (!payload.getPassword().equals(payload.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(errorBody(request, "password_not_match", msg(request, "password_not_match"), null));
        }
        Optional<UserInvitation> optional = userInvitationRepository.findByTokenAndUsedFalse(invitationToken);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "invitation_not_found", msg(request, "invitation_not_found"), null));
        }
        UserInvitation invitation = optional.get();
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(410).body(errorBody(request, "invitation_expired", msg(request, "invitation_expired"), null));
        }

        Optional<UserAccount> existing = userAccountRepository.findByUsernameAndTenantId(invitation.getUsername(), invitation.getTenantId());
        if (existing.isPresent()) {
            return ResponseEntity.status(409).body(errorBody(request, "username_exists", msg(request, "username_exists"), null));
        }

        UserAccount user = new UserAccount();
        user.setId("u_" + Long.toString(System.currentTimeMillis(), 36));
        user.setUsername(invitation.getUsername());
        user.setPassword(passwordEncoder.encode(payload.getPassword()));
        user.setRole(invitation.getRole());
        user.setDisplayName(isBlank(payload.getDisplayName()) ? invitation.getDisplayName() : payload.getDisplayName().trim());
        user.setOwnerScope(invitation.getOwnerScope());
        user.setEnabled(true);
        user.setTenantId(invitation.getTenantId());
        user.setDepartment(invitation.getDepartment());
        user.setDataScope(invitation.getDataScope());
        userAccountRepository.save(user);

        invitation.setUsed(true);
        invitation.setUsedAt(LocalDateTime.now());
        userInvitationRepository.save(invitation);
        auditLogService.record(user.getUsername(), user.getRole(), "INVITATION_ACCEPT", "AUTH", null, "Accepted invitation", user.getTenantId());
        Map<String, Object> body = successByKey(request, "invitation_accepted", null);
        body.put("tenantId", user.getTenantId());
        body.put("username", user.getUsername());
        body.put("displayName", user.getDisplayName());
        return ResponseEntity.status(201).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(HttpServletRequest request, @Valid @RequestBody V1AuthLoginRequest payload) {
        String tenantId = normalizeRequiredValue(payload.getTenantId());
        if (tenantId == null) {
            return validationError(request, "tenantId");
        }
        String username = normalizeRequiredValue(payload.getUsername());
        if (username == null) {
            return validationError(request, "username");
        }
        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();

        if (loginRiskService.isLocked(tenantId, username, ip)) {
            Map<String, Object> details = new HashMap<>();
            details.put("retryAfterSeconds", loginRiskService.remainingSeconds(tenantId, username, ip));
            return ResponseEntity.status(423).body(errorBody(request, "login_locked", msg(request, "login_locked"), details));
        }

        Optional<UserAccount> optional = userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue(username, tenantId);
        if (!optional.isPresent()) {
            loginRiskService.recordFailure(tenantId, username, ip);
            return ResponseEntity.status(401).body(errorBody(request, "invalid_credentials", msg(request, "invalid_credentials"), null));
        }

        UserAccount user = optional.get();
        if (!passwordEncoder.matches(payload.getPassword(), user.getPassword())) {
            loginRiskService.recordFailure(tenantId, username, ip);
            return ResponseEntity.status(401).body(errorBody(request, "invalid_credentials", msg(request, "invalid_credentials"), null));
        }

        if (mfaService.requiresMfa(user.getRole())) {
            if (!isBlank(payload.getMfaCode())) {
                if (!mfaService.verify(payload.getMfaCode())) {
                    loginRiskService.recordFailure(tenantId, username, ip);
                    return ResponseEntity.status(401).body(errorBody(request, "mfa_invalid", msg(request, "mfa_invalid"), null));
                }
                loginRiskService.clear(tenantId, username, ip);
                Map<String, Object> body = buildAuthBody(request, user, true);
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, sessionCookieService.buildSessionCookie(String.valueOf(body.get("token"))))
                        .body(body);
            }
            String challengeId = mfaChallengeService.issue(user.getUsername(), user.getRole(), safeOwner(user), user.getTenantId());
            Map<String, Object> body = new HashMap<>();
            body.put("code", "mfa_required");
            body.put("message", msg(request, "mfa_required"));
            body.put("mfaRequired", true);
            body.put("challengeId", challengeId);
            body.put("requestId", traceId(request));
            body.put("details", new LinkedHashMap<>());
            return ResponseEntity.status(202).body(body);
        }

        loginRiskService.clear(tenantId, username, ip);
        Map<String, Object> body = buildAuthBody(request, user, true);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieService.buildSessionCookie(String.valueOf(body.get("token"))))
                .body(body);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(HttpServletRequest request, @Valid @RequestBody V1MfaVerifyRequest payload) {
        String challengeId = normalizeRequiredValue(payload.getChallengeId());
        if (challengeId == null) {
            return validationError(request, "challengeId");
        }
        String code = normalizeRequiredValue(payload.getCode());
        if (code == null) {
            return validationError(request, "code");
        }
        if (!mfaService.verify(code)) {
            return ResponseEntity.status(401).body(errorBody(request, "mfa_invalid", msg(request, "mfa_invalid"), null));
        }
        MfaChallengeService.Challenge challenge = mfaChallengeService.consume(challengeId);
        if (challenge == null) {
            return ResponseEntity.status(400).body(errorBody(request, "mfa_challenge_invalid", msg(request, "mfa_challenge_invalid"), null));
        }

        Optional<UserAccount> optional = userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue(challenge.getUsername(), challenge.getTenantId());
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "user_not_found", msg(request, "user_not_found"), null));
        }
        Map<String, Object> body = buildAuthBody(request, optional.get(), true);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieService.buildSessionCookie(String.valueOf(body.get("token"))))
                .body(body);
    }

    @PostMapping("/oidc/callback")
    public ResponseEntity<?> oidcCallback(HttpServletRequest request, @Valid @RequestBody SsoLoginRequest payload) {
        if (!ssoAuthService.isEnabled()) {
            return ResponseEntity.status(403).body(errorBody(request, "sso_disabled", msg(request, "sso_disabled"), null));
        }
        SsoIdentity identity = ssoAuthService.resolveIdentity(payload);
        if (identity == null) {
            return ResponseEntity.status(401).body(errorBody(request, "sso_oidc_exchange_failed", msg(request, "sso_oidc_exchange_failed"), null));
        }

        String tenantId = normalizeRequiredValue(payload.getTenantId());
        if (tenantId == null) {
            return validationError(request, "tenantId");
        }
        if (!tenantRepository.findById(tenantId).isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "tenant_not_found", msg(request, "tenant_not_found"), null));
        }
        Optional<UserAccount> existing = userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue(identity.getUsername(), tenantId);
        UserAccount user;
        if (existing.isPresent()) {
            user = existing.get();
        } else {
            user = new UserAccount();
            user.setId("u_oidc_" + Long.toString(System.currentTimeMillis(), 36));
            user.setUsername(identity.getUsername());
            user.setPassword(passwordEncoder.encode(generateSecureRandomPassword()));
            user.setRole(ssoAuthService.defaultRole());
            user.setDisplayName(identity.getDisplayName());
            user.setOwnerScope(identity.getUsername());
            user.setEnabled(true);
            user.setTenantId(tenantId);
            user.setDepartment("DEFAULT");
            user.setDataScope("SELF");
            user = userAccountRepository.save(user);
        }
        Map<String, Object> body = buildAuthBody(request, user, true);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieService.buildSessionCookie(String.valueOf(body.get("token"))))
                .body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String username = String.valueOf(request.getAttribute("authUsername"));
        String role = String.valueOf(request.getAttribute("authRole"));
        String tenantId = String.valueOf(request.getAttribute("authTenantId"));
        auditLogService.record(username, role, "LOGOUT_V1", "AUTH", null, "User logged out via v1 auth", tenantId);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieService.buildClearCookie())
                .body(successByKey(request, "logout_success", null));
    }

    @GetMapping("/session")
    public ResponseEntity<?> session(HttpServletRequest request) {
        String tenantId = currentTenant(request);
        String username = currentUser(request);
        Optional<UserAccount> optional = userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue(username, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(401).body(errorBody(request, "unauthorized", msg(request, "invalid_or_expired"), null));
        }
        UserAccount user = optional.get();
        Map<String, Object> body = successByKey(request, "auth_success", null);
        body.put("token", "");
        body.put("username", user.getUsername());
        body.put("displayName", user.getDisplayName());
        body.put("role", user.getRole());
        body.put("ownerScope", safeOwner(user));
        body.put("tenantId", user.getTenantId());
        body.put("dateFormat", tenantRepository.findById(user.getTenantId()).map(t -> {
            String format = t.getDateFormat();
            if ("YYYY-MM-DD".equalsIgnoreCase(format)) return "yyyy-MM-dd";
            return isBlank(format) ? "yyyy-MM-dd" : format;
        }).orElse("yyyy-MM-dd"));
        body.put("department", user.getDepartment());
        body.put("dataScope", user.getDataScope());
        body.put("mfaVerified", true);
        body.put("requestId", traceId(request));
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> buildAuthBody(HttpServletRequest request, UserAccount user, boolean mfaVerified) {
        String token = tokenService.createToken(user.getUsername(), user.getRole(), safeOwner(user), user.getTenantId(), mfaVerified);
        Map<String, Object> body = successByKey(request, "auth_success", null);
        body.put("token", token);
        body.put("username", user.getUsername());
        body.put("displayName", user.getDisplayName());
        body.put("role", user.getRole());
        body.put("ownerScope", safeOwner(user));
        body.put("tenantId", user.getTenantId());
        String tenantDateFormat = tenantRepository.findById(user.getTenantId()).map(t -> t.getDateFormat()).orElse("yyyy-MM-dd");
        if ("YYYY-MM-DD".equalsIgnoreCase(tenantDateFormat)) {
            tenantDateFormat = "yyyy-MM-dd";
        }
        body.put("dateFormat", tenantDateFormat);
        body.put("department", user.getDepartment());
        body.put("dataScope", user.getDataScope());
        body.put("mfaVerified", mfaVerified);
        body.put("requestId", traceId(request));
        auditLogService.record(user.getUsername(), user.getRole(), "LOGIN_V1", "AUTH", null, "User logged in via v1 auth", user.getTenantId());
        return body;
    }

    private String safeOwner(UserAccount user) {
        return isBlank(user.getOwnerScope()) ? user.getUsername() : user.getOwnerScope();
    }

    private String normalizeRequiredValue(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String normalizeOptionalValue(String value, String fallback) {
        String normalized = normalizeRequiredValue(value);
        return normalized == null ? fallback : normalized;
    }

    private ResponseEntity<?> validationError(HttpServletRequest request, String field) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("field", field);
        return ResponseEntity.badRequest().body(errorBody(request, "validation_error", msg(request, "validation_error"), details));
    }

    private String generateSecureRandomPassword() {
        // Generate a strong random password that meets common password policy requirements
        // Format: UUID-based + suffix with uppercase, lowercase, digit, and special char
        return UUID.randomUUID().toString().replace("-", "") + "!Aa1";
    }
}
