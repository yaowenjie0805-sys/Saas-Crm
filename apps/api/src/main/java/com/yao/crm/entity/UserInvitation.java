package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_invitations")
public class UserInvitation {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 96, unique = true)
    private String token;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(length = 120)
    private String ownerScope;

    @Column(length = 80)
    private String department;

    @Column(length = 30)
    private String dataScope;

    @Column(length = 80)
    private String displayName;

    @Column(nullable = false, length = 80)
    private String invitedBy;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used;

    private LocalDateTime usedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
        if (role == null || role.trim().isEmpty()) role = "SALES";
        if (department == null || department.trim().isEmpty()) department = "DEFAULT";
        if (dataScope == null || dataScope.trim().isEmpty()) dataScope = "SELF";
        if (used == null) used = false;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getOwnerScope() { return ownerScope; }
    public void setOwnerScope(String ownerScope) { this.ownerScope = ownerScope; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getInvitedBy() { return invitedBy; }
    public void setInvitedBy(String invitedBy) { this.invitedBy = invitedBy; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
