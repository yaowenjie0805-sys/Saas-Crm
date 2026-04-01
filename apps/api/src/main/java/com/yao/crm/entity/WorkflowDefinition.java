package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流定义实体
 */
@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinition {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 40)
    private String category;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 80)
    private String owner;

    @Column(length = 80)
    private String department;

    @Column(nullable = false)
    private Boolean isSystem;

    @Column(nullable = false)
    private Integer executionCount;

    @Column
    private LocalDateTime lastExecutedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime activatedAt;

    @Column(length = 80)
    private String publishedBy;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (version == null) version = 1;
        if (isSystem == null) isSystem = false;
        if (executionCount == null) executionCount = 0;
        if (status == null || status.trim().isEmpty()) status = "DRAFT";
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public Integer getExecutionCount() { return executionCount; }
    public void setExecutionCount(Integer executionCount) { this.executionCount = executionCount; }

    public LocalDateTime getLastExecutedAt() { return lastExecutedAt; }
    public void setLastExecutedAt(LocalDateTime lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }

    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }
}
