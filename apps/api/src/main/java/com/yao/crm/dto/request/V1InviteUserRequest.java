package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class V1InviteUserRequest {

    @NotBlank(message = "register_username_required")
    @Size(min = 4, max = 40, message = "register_username_length")
    private String username;

    @NotBlank(message = "invalid_role")
    @Pattern(regexp = "ADMIN|MANAGER|SALES|ANALYST", message = "invalid_role")
    private String role;

    @Size(max = 120, message = "owner_scope_too_long")
    private String ownerScope;

    @Size(max = 80, message = "display_name_too_long")
    private String displayName;

    @Size(max = 80, message = "bad_request")
    private String department;

    @Pattern(regexp = "SELF|TEAM|DEPT|ALL", message = "bad_request")
    private String dataScope;

    private Integer expiresInHours;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getOwnerScope() { return ownerScope; }
    public void setOwnerScope(String ownerScope) { this.ownerScope = ownerScope; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public Integer getExpiresInHours() { return expiresInHours; }
    public void setExpiresInHours(Integer expiresInHours) { this.expiresInHours = expiresInHours; }
}
