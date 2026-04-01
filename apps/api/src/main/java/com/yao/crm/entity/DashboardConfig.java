package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 浠〃鐩橀厤缃疄浣?
 */
@Entity
@Table(name = "dashboard_configs")
public class DashboardConfig {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String layoutJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String widgetsJson;

    @Column(nullable = false, length = 20)
    private String visibility;

    @Column(nullable = false, length = 80)
    private String owner;

    @Column(nullable = false)
    private Boolean isDefault;

    @Column(nullable = false)
    private Boolean isSystem;

    @Column(nullable = false)
    private Integer version;

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
        if (version == null) version = 1;
        if (isDefault == null) isDefault = false;
        if (isSystem == null) isSystem = false;
        if (visibility == null || visibility.trim().isEmpty()) visibility = "PRIVATE";
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        version = version + 1;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLayoutJson() {
        return layoutJson;
    }

    public void setLayoutJson(String layoutJson) {
        this.layoutJson = layoutJson;
    }

    public String getWidgetsJson() {
        return widgetsJson;
    }

    public void setWidgetsJson(String widgetsJson) {
        this.widgetsJson = widgetsJson;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
