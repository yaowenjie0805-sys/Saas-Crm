package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 活动分享实体 - 记录客户动态分享
 */
@Entity
@Table(name = "activity_shares")
public class ActivityShare {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 20)
    private String shareType;

    @Column(nullable = false, length = 80)
    private String shareTarget;

    @Column(nullable = false, length = 40)
    private String activityType;

    @Column(nullable = false, length = 40)
    private String entityType;

    @Column(nullable = false, length = 64)
    private String entityId;

    @Column(nullable = false, length = 80)
    private String sharedBy;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (tenantId == null || tenantId.trim().isEmpty()) tenantId = "tenant_default";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getShareType() { return shareType; }
    public void setShareType(String shareType) { this.shareType = shareType; }

    public String getShareTarget() { return shareTarget; }
    public void setShareTarget(String shareTarget) { this.shareTarget = shareTarget; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getSharedBy() { return sharedBy; }
    public void setSharedBy(String sharedBy) { this.sharedBy = sharedBy; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
