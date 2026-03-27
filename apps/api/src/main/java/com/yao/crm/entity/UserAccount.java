package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(nullable = false, length = 80)
    private String displayName;

    @Column(length = 120)
    private String ownerScope;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(length = 80)
    private String department;

    @Column(length = 30)
    private String dataScope;

    @PrePersist
    public void prePersist() {
        if (tenantId == null || tenantId.trim().isEmpty()) tenantId = "tenant_default";
        if (department == null || department.trim().isEmpty()) department = "DEFAULT";
        if (dataScope == null || dataScope.trim().isEmpty()) dataScope = "SELF";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }
}
