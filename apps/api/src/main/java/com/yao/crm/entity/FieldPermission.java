package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 字段权限实体
 * 支持字段级的查看、编辑、删除权限控制
 */
@Entity
@Table(name = "field_permissions")
public class FieldPermission {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 40)
    private String entityType;

    @Column(nullable = false, length = 80)
    private String fieldName;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(nullable = false)
    private Boolean canView;

    @Column(nullable = false)
    private Boolean canEdit;

    @Column(nullable = false)
    private Boolean canDelete;

    @Column(nullable = false)
    private Boolean isHidden;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (canView == null) canView = true;
        if (canEdit == null) canEdit = false;
        if (canDelete == null) canDelete = false;
        if (isHidden == null) isHidden = false;
        if (tenantId == null || tenantId.trim().isEmpty()) tenantId = "tenant_default";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getCanView() { return canView; }
    public void setCanView(Boolean canView) { this.canView = canView; }

    public Boolean getCanEdit() { return canEdit; }
    public void setCanEdit(Boolean canEdit) { this.canEdit = canEdit; }

    public Boolean getCanDelete() { return canDelete; }
    public void setCanDelete(Boolean canDelete) { this.canDelete = canDelete; }

    public Boolean getIsHidden() { return isHidden; }
    public void setIsHidden(Boolean isHidden) { this.isHidden = isHidden; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
