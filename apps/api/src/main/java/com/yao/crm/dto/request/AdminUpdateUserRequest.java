package com.yao.crm.dto.request;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AdminUpdateUserRequest {

    @Pattern(regexp = "(?i)ADMIN|MANAGER|SALES|ANALYST", message = "invalid_role")
    private String role;

    @Size(max = 64, message = "owner_scope_too_long")
    private String ownerScope;

    private Boolean enabled;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOwnerScope() {
        return ownerScope;
    }

    public void setOwnerScope(String ownerScope) {
        this.ownerScope = ownerScope;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @AssertTrue(message = "admin_update_payload_empty")
    public boolean isAnyFieldProvided() {
        return role != null || ownerScope != null || enabled != null;
    }
}