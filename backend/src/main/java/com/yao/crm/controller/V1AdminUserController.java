package com.yao.crm.controller;

import com.yao.crm.dto.request.AdminUpdateUserRequest;
import com.yao.crm.dto.request.V1InviteUserRequest;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.entity.UserInvitation;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.repository.UserInvitationRepository;
import com.yao.crm.security.LoginRiskService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/users")
public class V1AdminUserController extends BaseApiController {

    private static final Set<String> ROLES = new HashSet<String>(Arrays.asList("ADMIN", "MANAGER", "SALES", "ANALYST"));
    private static final Set<String> DATA_SCOPES = new HashSet<String>(Arrays.asList("SELF", "DEPARTMENT", "GLOBAL"));

    private final UserAccountRepository userAccountRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final LoginRiskService loginRiskService;
    private final AuditLogService auditLogService;

    public V1AdminUserController(UserAccountRepository userAccountRepository,
                                 UserInvitationRepository userInvitationRepository,
                                 LoginRiskService loginRiskService,
                                 AuditLogService auditLogService,
                                 I18nService i18nService) {
        super(i18nService);
        this.userAccountRepository = userAccountRepository;
        this.userInvitationRepository = userInvitationRepository;
        this.loginRiskService = loginRiskService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN")) {
            return forbidden(request);
        }
        String tenantId = currentTenant(request);
        List<UserAccount> users = userAccountRepository.findAllByTenantIdOrderByUsernameAsc(tenantId);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (UserAccount user : users) {
            items.add(toView(user));
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("items", items);
        return ResponseEntity.ok(successWithFields(request, "users_listed", body));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patchUser(HttpServletRequest request,
                                       @PathVariable String id,
                                       @Valid @RequestBody AdminUpdateUserRequest payload) {
        if (!hasAnyRole(request, "ADMIN")) {
            return forbidden(request);
        }
        String tenantId = currentTenant(request);
        Optional<UserAccount> optional = userAccountRepository.findById(id);
        if (!optional.isPresent() || !tenantId.equals(optional.get().getTenantId())) {
            return notFound(request, "user_not_found");
        }

        UserAccount user = optional.get();
        if (payload.getRole() != null) {
            String role = payload.getRole().trim().toUpperCase(Locale.ROOT);
            if (!ROLES.contains(role)) {
                return badRequest(request, "invalid_role");
            }
            user.setRole(role);
        }
        if (payload.getOwnerScope() != null) user.setOwnerScope(payload.getOwnerScope().trim());
        if (payload.getEnabled() != null) user.setEnabled(payload.getEnabled());
        user = userAccountRepository.save(user);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "USER", user.getId(), "Updated user by v1 admin API", tenantId);
        return ResponseEntity.ok(successWithFields(request, "user_updated", toView(user)));
    }

    @PostMapping("/invite")
    public ResponseEntity<?> inviteUser(HttpServletRequest request, @Valid @RequestBody V1InviteUserRequest payload) {
        if (!hasAnyRole(request, "ADMIN")) {
            return forbidden(request);
        }
        String tenantId = currentTenant(request);
        String role = payload.getRole().trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(role)) {
            return badRequest(request, "invalid_role");
        }
        String dataScope = isBlank(payload.getDataScope()) ? "SELF" : payload.getDataScope().trim().toUpperCase(Locale.ROOT);
        if (!DATA_SCOPES.contains(dataScope)) {
            return badRequest(request, "bad_request");
        }
        String username = payload.getUsername().trim();
        if (userAccountRepository.findByUsernameAndTenantId(username, tenantId).isPresent()) {
            return badRequest(request, "username_exists");
        }

        UserInvitation invitation = new UserInvitation();
        invitation.setId("uinv_" + Long.toString(System.currentTimeMillis(), 36));
        invitation.setToken(UUID.randomUUID().toString().replace("-", ""));
        invitation.setTenantId(tenantId);
        invitation.setUsername(username);
        invitation.setRole(role);
        invitation.setOwnerScope(isBlank(payload.getOwnerScope()) ? username : payload.getOwnerScope().trim());
        invitation.setDepartment(isBlank(payload.getDepartment()) ? "DEFAULT" : payload.getDepartment().trim());
        invitation.setDataScope(dataScope);
        invitation.setDisplayName(isBlank(payload.getDisplayName()) ? username : payload.getDisplayName().trim());
        invitation.setInvitedBy(currentUser(request));
        int expiresHours = payload.getExpiresInHours() == null ? 72 : Math.max(1, Math.min(payload.getExpiresInHours(), 168));
        invitation.setExpiresAt(LocalDateTime.now().plusHours(expiresHours));
        invitation = userInvitationRepository.save(invitation);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("invitationId", invitation.getId());
        body.put("token", invitation.getToken());
        body.put("tenantId", invitation.getTenantId());
        body.put("username", invitation.getUsername());
        body.put("expiresAt", invitation.getExpiresAt());
        body.put("inviteLink", "/activate?token=" + invitation.getToken());
        auditLogService.record(currentUser(request), currentRole(request), "INVITE", "USER_INVITATION", invitation.getId(), "Invited user " + invitation.getUsername(), tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "invitation_created", body));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<?> unlockUser(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN")) {
            return forbidden(request);
        }
        String tenantId = currentTenant(request);
        Optional<UserAccount> optional = userAccountRepository.findById(id);
        if (!optional.isPresent() || !tenantId.equals(optional.get().getTenantId())) {
            return notFound(request, "user_not_found");
        }
        UserAccount user = optional.get();
        loginRiskService.clearUser(user.getUsername());
        auditLogService.record(currentUser(request), currentRole(request), "UNLOCK", "USER", user.getId(), "Unlocked user in v1 admin API", tenantId);
        return ResponseEntity.ok(successWithFields(request, "user_unlocked", toView(user)));
    }

    private Map<String, Object> toView(UserAccount user) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", user.getId());
        out.put("username", user.getUsername());
        out.put("displayName", user.getDisplayName());
        out.put("role", user.getRole());
        out.put("ownerScope", isBlank(user.getOwnerScope()) ? "" : user.getOwnerScope());
        out.put("department", isBlank(user.getDepartment()) ? "" : user.getDepartment());
        out.put("dataScope", isBlank(user.getDataScope()) ? "SELF" : user.getDataScope());
        out.put("enabled", Boolean.TRUE.equals(user.getEnabled()));
        out.put("locked", loginRiskService.isUserLocked(user.getUsername()));
        out.put("lockRemainingSeconds", loginRiskService.remainingUserSeconds(user.getUsername()));
        return out;
    }

    private ResponseEntity<?> forbidden(HttpServletRequest request) {
        return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
    }

    private ResponseEntity<?> badRequest(HttpServletRequest request, String msgKey) {
        String code = normalizeCode(msgKey, "bad_request");
        return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
    }

    private ResponseEntity<?> notFound(HttpServletRequest request, String msgKey) {
        String code = normalizeCode(msgKey, "not_found");
        return ResponseEntity.status(404).body(errorBody(request, code, msg(request, code), null));
    }
}

