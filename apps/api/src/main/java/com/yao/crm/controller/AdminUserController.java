package com.yao.crm.controller;

import com.yao.crm.dto.request.AdminUpdateUserRequest;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.security.LoginRiskService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController extends BaseApiController {

    private static final Set<String> ROLES = new HashSet<String>(Arrays.asList("ADMIN", "MANAGER", "SALES", "ANALYST"));

    private final UserAccountRepository userAccountRepository;
    private final LoginRiskService loginRiskService;
    private final AuditLogService auditLogService;

    public AdminUserController(UserAccountRepository userAccountRepository,
                               LoginRiskService loginRiskService,
                               AuditLogService auditLogService,
                               I18nService i18nService) {
        super(i18nService);
        this.userAccountRepository = userAccountRepository;
        this.loginRiskService = loginRiskService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        List<UserAccount> all = userAccountRepository.findAll();
        all.sort(new Comparator<UserAccount>() {
            @Override
            public int compare(UserAccount a, UserAccount b) {
                return String.valueOf(a.getUsername()).compareToIgnoreCase(String.valueOf(b.getUsername()));
            }
        });

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (UserAccount user : all) {
            items.add(toView(user));
        }
        return ResponseEntity.ok(Collections.singletonMap("items", items));
    }

    @PatchMapping("/users/{username}")
    public ResponseEntity<?> updateUser(HttpServletRequest request,
                                        @PathVariable String username,
                                        @Valid @RequestBody AdminUpdateUserRequest payload) {
        if (!hasAnyRole(request, "ADMIN")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        Optional<UserAccount> optional = userAccountRepository.findByUsername(username);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "user_not_found", "NOT_FOUND", null));
        }
        UserAccount user = optional.get();

        if (payload.getRole() != null) {
            String role = payload.getRole().trim().toUpperCase(Locale.ROOT);
            if (!ROLES.contains(role)) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_role", "BAD_REQUEST", null));
            }
            user.setRole(role);
            if (!"SALES".equals(role)) {
                user.setOwnerScope("");
            }
        }

        if (payload.getOwnerScope() != null) {
            if ("SALES".equals(user.getRole())) {
                user.setOwnerScope(payload.getOwnerScope().trim());
            }
        }

        if (payload.getEnabled() != null) {
            user.setEnabled(payload.getEnabled());
        }

        if ("SALES".equals(user.getRole()) && isBlank(user.getOwnerScope())) {
            user.setOwnerScope(user.getUsername());
        }

        UserAccount saved = userAccountRepository.save(user);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "USER", saved.getUsername(), "Updated user governance settings");
        return ResponseEntity.ok(toView(saved));
    }

    @PostMapping("/users/{username}/unlock")
    public ResponseEntity<?> unlockUser(HttpServletRequest request, @PathVariable String username) {
        if (!hasAnyRole(request, "ADMIN")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        Optional<UserAccount> optional = userAccountRepository.findByUsername(username);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "user_not_found", "NOT_FOUND", null));
        }

        loginRiskService.clearUser(username);
        UserAccount user = optional.get();
        auditLogService.record(currentUser(request), currentRole(request), "UNLOCK", "USER", user.getUsername(), "Cleared login risk lock");
        return ResponseEntity.ok(toView(user));
    }

    private Map<String, Object> toView(UserAccount user) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("username", user.getUsername());
        out.put("displayName", user.getDisplayName());
        out.put("role", user.getRole());
        out.put("ownerScope", user.getOwnerScope() == null ? "" : user.getOwnerScope());
        out.put("enabled", Boolean.TRUE.equals(user.getEnabled()));
        boolean locked = loginRiskService.isUserLocked(user.getUsername());
        out.put("locked", locked);
        out.put("lockRemainingSeconds", loginRiskService.remainingUserSeconds(user.getUsername()));
        return out;
    }
}


