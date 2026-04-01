package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 鍥捐〃妯℃澘瀹炰綋
 * 鏀寔鏌辩姸鍥俱€佹姌绾垮浘銆侀ゼ鍥俱€佹紡鏂楀浘绛夊浘琛ㄧ被鍨?
 */
@Entity
@Table(name = "chart_templates")
public class ChartTemplate {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 40)
    private String chartType;

    @Column(nullable = false, length = 40)
    private String datasetType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(columnDefinition = "TEXT")
    private String layoutConfig;

    @Column(nullable = false, length = 20)
    private String visibility;

    @Column(nullable = false, length = 80)
    private String owner;

    @Column(length = 80)
    private String department;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private Boolean isSystem;

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

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String chartType) {
        this.chartType = chartType;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getLayoutConfig() {
        return layoutConfig;
    }

    public void setLayoutConfig(String layoutConfig) {
        this.layoutConfig = layoutConfig;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
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
