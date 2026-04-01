package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 80)
    private String resource;

    @Column(length = 64)
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalStateException("tenant_id_required");
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
