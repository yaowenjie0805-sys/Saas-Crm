package com.yao.crm.controller;

import com.yao.crm.dto.request.UpdateRolePermissionsRequest;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.PermissionMatrixService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PermissionController extends BaseApiController {

    private final PermissionMatrixService permissionMatrixService;
    private final AuditLogService auditLogService;

    public PermissionController(PermissionMatrixService permissionMatrixService,
                                AuditLogService auditLogService,
                                I18nService i18nService) {
        super(i18nService);
        this.permissionMatrixService = permissionMatrixService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/permissions/matrix")
    public ResponseEntity<?> matrix(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("roles", permissionMatrixService.roles());
        body.put("matrix", permissionMatrixService.matrixRows());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/permissions/conflicts")
    public ResponseEntity<?> conflicts(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        return ResponseEntity.ok(Collections.singletonMap("items", permissionMatrixService.conflicts()));
    }

    @PostMapping("/permissions/roles/{role}/preview")
    public ResponseEntity<?> previewRole(HttpServletRequest request,
                                         @PathVariable String role,
                                         @Valid @RequestBody UpdateRolePermissionsRequest payload) {
        if (!hasAnyRole(request, "ADMIN")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        try {
            return ResponseEntity.ok(permissionMatrixService.previewRole(role, payload.getGrant(), payload.getRevoke()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(legacyErrorByKnownKey(
                    request,
                    ex.getMessage(),
                    "bad_request",
                    "BAD_REQUEST",
                    LEGACY_PERMISSION_ERROR_KEYS,
                    null
            ));
        }
    }

    @PatchMapping("/permissions/roles/{role}")
    public ResponseEntity<?> updateRole(HttpServletRequest request,
                                        @PathVariable String role,
                                        @Valid @RequestBody UpdateRolePermissionsRequest payload) {
        if (!hasAnyRole(request, "ADMIN")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        try {
            Map<String, Object> body = permissionMatrixService.updateRole(role, payload.getGrant(), payload.getRevoke());
            auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "PERMISSION", role.toUpperCase(), "Updated permission matrix", currentTenant(request));
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(legacyErrorByKnownKey(
                    request,
                    ex.getMessage(),
                    "bad_request",
                    "BAD_REQUEST",
                    LEGACY_PERMISSION_ERROR_KEYS,
                    null
            ));
        }
    }

    @PostMapping("/permissions/roles/{role}/rollback")
    public ResponseEntity<?> rollbackRole(HttpServletRequest request, @PathVariable String role) {
        if (!hasAnyRole(request, "ADMIN")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        try {
            Map<String, Object> body = permissionMatrixService.rollbackRole(role);
            auditLogService.record(currentUser(request), currentRole(request), "ROLLBACK", "PERMISSION", role.toUpperCase(), "Rolled back permission matrix", currentTenant(request));
            return ResponseEntity.ok(body);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(legacyErrorByKnownKey(
                    request,
                    ex.getMessage(),
                    "permission_rollback_empty",
                    "CONFLICT",
                    LEGACY_PERMISSION_ERROR_KEYS,
                    null
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(legacyErrorByKnownKey(
                    request,
                    ex.getMessage(),
                    "bad_request",
                    "BAD_REQUEST",
                    LEGACY_PERMISSION_ERROR_KEYS,
                    null
            ));
        }
    }
}

